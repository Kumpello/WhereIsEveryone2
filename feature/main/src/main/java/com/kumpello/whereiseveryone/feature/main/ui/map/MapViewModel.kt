package com.kumpello.whereiseveryone.feature.main.ui.map

import com.kumpello.whereiseveryone.feature.main.location.LocationService
import com.kumpello.whereiseveryone.feature.main.R
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.core.presentation.BaseViewModel
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.feature.main.ui.mapper.LocationUtils
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapFriendUseCase
import com.kumpello.whereiseveryone.feature.main.ui.mapper.MapLocationUseCase
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.kumpello.whereiseveryone.data.model.FriendLocalData
import com.kumpello.whereiseveryone.feature.main.ui.model.Location
import com.kumpello.whereiseveryone.feature.main.ui.model.MapFpsLimit
import com.kumpello.whereiseveryone.data.model.LocationData
import com.kumpello.whereiseveryone.data.model.MAX_POINTS_OF_INTEREST
import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.data.repository.PointsOfInterestRepository
import com.kumpello.whereiseveryone.core.util.formatDistance
import retrofit2.HttpException
import com.kumpello.whereiseveryone.data.model.toLocalData
import com.kumpello.whereiseveryone.data.network.model.SharingResponse
import com.kumpello.whereiseveryone.feature.main.domain.usecase.GetPausedFriendsUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.ResumeSharingUseCase
import com.kumpello.whereiseveryone.feature.main.domain.usecase.StopSharingUseCase
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.distinctUntilChanged
import com.kumpello.whereiseveryone.data.repository.RoadNavigationRepository
import com.kumpello.whereiseveryone.data.repository.RoadMatch
import com.kumpello.whereiseveryone.data.repository.RoadTravelEstimate
import com.kumpello.whereiseveryone.data.repository.RoadInterceptionEstimate
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import timber.log.Timber

class MapViewModel(
    private val locationService: LocationService,
    private val friendsManager: FriendsStateRepository,
    private val mapLocationUseCase: MapLocationUseCase,
    private val mapFriendUseCase: MapFriendUseCase,
    private val stopSharingUseCase: StopSharingUseCase,
    private val resumeSharingUseCase: ResumeSharingUseCase,
    private val getPausedFriendsUseCase: GetPausedFriendsUseCase,
    private val preferencesManager: PreferencesManager,
    private val roadNavigationRepository: RoadNavigationRepository,
    private val pointsOfInterestRepository: PointsOfInterestRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : BaseViewModel<MapViewModel.State, MapViewModel.ViewState, MapViewModel.Event, MapViewModel.Action>(
    State(),
    viewStateDispatcher = defaultDispatcher
) {
    private var roadUpdatesJob: Job? = null

    init {
        viewModelScope.launch {
            locationService.observeLocation().collect { location ->
                trigger(Event.OnLocationUpdate(location?.let {
                    LocationData(
                        lat = it.latitude,
                        lon = it.longitude,
                        bearing = it.bearing.takeIf { _ -> it.hasBearing() },
                        alt = it.altitude.takeIf { _ -> it.hasAltitude() },
                        accuracy = it.accuracy.takeIf { _ -> it.hasAccuracy() },
                        speed = it.speed.takeIf { _ -> it.hasSpeed() },
                        lastUpdate = it.time,
                    )
                }))
            }
        }
        viewModelScope.launch {
            preferencesManager.observe(PreferencesKey.CarMode).distinctUntilChanged().collect {
                trigger(Event.OnCarModeChanged(it == true))
            }
        }
        viewModelScope.launch {
            preferencesManager.observe(PreferencesKey.MapFpsLimit).distinctUntilChanged().collect {
                trigger(Event.OnMapFpsLimitChanged(MapFpsLimit.fromPreference(it)))
            }
        }
        viewModelScope.launch {
            try {
                friendsManager.observeFriends().collect { response ->
                    val event = when (response) {
                        is FriendsResponse.FriendsData -> {
                            val friends = withContext(defaultDispatcher) {
                                response.positions.map { it.toLocalData() }
                            }
                            Event.OnFriendsLoaded(friends)
                        }
                        is FriendsResponse.ErrorData -> Event.OnError(R.string.error_getting_friends)
                    }
                    trigger(event)
                    trigger(Event.CheckPaused)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Unable to observe friends")
                trigger(Event.OnError(R.string.error_getting_friends))
            }
        }
    }

    override fun reduce(state: State, event: Event): ReducerResult<State, Event, Action> {
        return when (event) {
            is Event.OnMapFpsLimitChanged -> state.copy(
                mapSettings = state.mapSettings.copy(fpsLimit = event.limit),
            ).toResult()
            is Event.OnLocationUpdate -> state.copy(
                user = event.location?.copy(isCarMode = state.isCarMode),
                showCarSuggestion = state.showCarSuggestion ||
                    (state.isMapVisible && state.carModeLoaded && !state.isCarMode &&
                        currentTimeMillis() >= state.suggestCarAfter && shouldSuggestCarMode(event.location, currentTimeMillis())),
            ).toResult()
            is Event.OnFriendsLoaded -> {
                val target = state.navigatingFriend?.username
                val wasDriver = state.friends.find { it.username == target }?.location?.isCarMode == true
                val isDriver = event.friends.find { it.username == target }?.location?.isCarMode == true
                val updated = state.copy(
                    friends = event.friends,
                    navigatingFriend = state.navigatingFriend?.takeIf { event.friends.any { friend -> friend.username == it.username && friend.location != null } },
                    selectedFriend = state.selectedFriend?.takeIf { event.friends.any { friend -> friend.username == it.username } },
                )
                if (wasDriver != isDriver || updated.navigatingFriend != state.navigatingFriend) {
                    updated.copy(friendRoad = null, travelEstimate = null, interceptionPrediction = null).restartRoadWork()
                }
                else updated.toResult()
            }
            is Event.OnCarModeChanged -> state.copy(
                isCarMode = event.enabled,
                carModeLoaded = true,
                user = state.user?.copy(isCarMode = event.enabled),
                showCarSuggestion = if (event.enabled) false else state.showCarSuggestion,
                suggestCarAfter = if (!event.enabled && state.isCarMode) currentTimeMillis() + 3_600_000L else state.suggestCarAfter,
                userRoad = if (event.enabled) state.userRoad else null,
                travelEstimate = null,
                interceptionPrediction = null,
            ).restartRoadWork()
            Event.AcceptCarSuggestion -> if (!state.showCarSuggestion) state.toResult() else state.copy(showCarSuggestion = false, suggestCarAfter = currentTimeMillis() + 3_600_000L)
                .toResult(SideEffect.AsyncWork {
                    try {
                        preferencesManager.save(PreferencesKey.CarMode, true)
                        Event.NoOp
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Event.OnError(R.string.car_mode_save_error)
                    }
                })
            Event.DismissCarSuggestion -> state.copy(showCarSuggestion = false, suggestCarAfter = currentTimeMillis() + 3_600_000L).toResult()
            is Event.OnMapVisibilityChanged -> state.copy(
                isMapVisible = event.visible, travelEstimate = null, interceptionPrediction = null,
                roadVersion = state.roadVersion + 1,
            )
                .toResult(roadWork(), SideEffect.InternalEvent(if (event.visible) Event.LoadOwnPoints else Event.NoOp))
            is Event.CheckCarFreshness -> if (state.isMapVisible) state.copy(freshnessCheckTime = event.time).toResult() else state.toResult()
            is Event.OnRoadUpdate -> {
                // A cancelled request cannot update a different navigation target or disabled mode.
                if (!state.isMapVisible || state.navigatingFriend?.username != event.target || state.roadVersion != event.version) state.toResult()
                else state.copy(
                    userRoad = if (state.isCarMode) event.userMatch ?: state.userRoad else null,
                    friendRoad = event.friendMatch ?: state.friendRoad,
                    travelEstimate = event.estimate ?: state.travelEstimate,
                    travelEstimateTime = if (event.estimate != null) event.estimatedAt else state.travelEstimateTime,
                    interceptionPrediction = event.interception ?: state.interceptionPrediction,
                ).toResult()
            }

            is Event.OnPausedFriendsLoaded -> state.copy(pausedFriends = event.pausedFriends).toResult()

            is Event.ToggleSharing -> {
                val isPaused = state.pausedFriends.contains(event.nick)
                val (useCase, loadingMsg, successMsg, errorMsg) = if (isPaused) {
                    listOf(
                        resumeSharingUseCase::execute,
                        R.string.resume_sharing,
                        R.string.sharing_resumed_successfully,
                        R.string.error_resuming_sharing
                    )
                } else {
                    listOf(
                        stopSharingUseCase::execute,
                        R.string.stop_sharing,
                        R.string.sharing_stopped_successfully,
                        R.string.error_stopping_sharing
                    )
                }

                state.toResult(
                    SideEffect.Effect(Action.Toast(loadingMsg as Int)),
                    SideEffect.AsyncWork {
                        try {
                            val response =
                                (useCase as suspend (String) -> CodeResponse).invoke(
                                    event.nick
                                )
                            if (response is CodeResponse.SuccessNoContent) {
                                Event.OnActionSuccess(successMsg as Int)
                            } else {
                                Event.OnError(errorMsg as Int)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.tag(TAG).w(e, "Unable to change sharing")
                            Event.OnError(errorMsg as Int)
                        }
                    })
            }

            is Event.OnActionSuccess -> state.toResult(
                SideEffect.Effect(Action.Toast(event.messageId)),
                SideEffect.InternalEvent(Event.CheckPaused)
            )

            Event.CheckPaused -> state.toResult(SideEffect.AsyncWork { checkPaused() })

            Event.NoOp -> state.toResult()

            is Event.OnError -> state.toResult(SideEffect.Effect(Action.Toast(event.id)))

            Event.CenterMap -> {
                Timber.tag(TAG).d("Centering map, zoom: %s", state.mapSettings.zoom)
                state.toResult(SideEffect.Effect(Action.CenterMap(state.mapSettings.zoom)))
            }

            Event.ZoomIn -> {
                val newZoom = state.mapSettings.zoom + 0.5
                Timber.tag(TAG).d("Zooming in, new zoom: %s", newZoom)
                state.copy(mapSettings = state.mapSettings.copy(zoom = newZoom)).toResult(
                    SideEffect.Effect(Action.Zoom(newZoom))
                )
            }

            Event.ZoomOut -> {
                val newZoom = state.mapSettings.zoom - 0.5
                Timber.tag(TAG).d("Zooming out, new zoom: %s", newZoom)
                state.copy(mapSettings = state.mapSettings.copy(zoom = newZoom)).toResult(
                    SideEffect.Effect(Action.Zoom(newZoom))
                )
            }

            is Event.OnFriendClick -> state.copy(selectedFriend = event.friend).toResult()
            is Event.OnFriendLongClick -> state.copy(selectedFriend = event.friend).toResult()
            Event.DismissFriendDetails -> state.copy(selectedFriend = null).toResult()
            is Event.NavigateToFriend -> state.copy(
                navigatingFriend = event.friend,
                pointsOfInterest = state.pointsOfInterest.copy(navigatingId = null),
                selectedFriend = null,
                friendRoad = null,
                travelEstimate = null,
                interceptionPrediction = null,
            ).restartRoadWork()

            Event.CancelNavigation -> state.copy(
                navigatingFriend = null, friendRoad = null, travelEstimate = null, interceptionPrediction = null,
                pointsOfInterest = state.pointsOfInterest.copy(navigatingId = null),
            ).restartRoadWork()
            is Event.OnCameraUpdate -> state.copy(
                mapSettings = state.mapSettings.copy(bearing = event.bearing)
            ).toResult()
            Event.LoadOwnPoints -> if (state.pointsOfInterest.isLoading || state.pointsOfInterest.isSaving) state.toResult()
                else state.copy(pointsOfInterest = state.pointsOfInterest.copy(isLoading = true, error = null))
                    .toResult(SideEffect.AsyncWork {
                        try {
                            Event.OnOwnPointsLoaded(pointsOfInterestRepository.getOwnPoints())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            Event.OnPointError(R.string.poi_load_error)
                        }
                    })
            is Event.OnOwnPointsLoaded -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                ownPoints = event.points, isLoaded = true, isLoading = false,
            )).toResult()
            Event.ShowPointManager -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(showManager = true))
                .toResult(SideEffect.InternalEvent(Event.LoadOwnPoints))
            Event.DismissPointManager -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(showManager = false)).toResult()
            is Event.SetPointLayerVisible -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(showLayer = event.visible)).toResult()
            is Event.OnMapLongClick -> when {
                state.pointsOfInterest.isSaving -> state.toResult()
                !state.pointsOfInterest.isLoaded -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(showManager = true))
                    .toResult(SideEffect.InternalEvent(Event.LoadOwnPoints))
                state.pointsOfInterest.ownPoints.size >= MAX_POINTS_OF_INTEREST -> state.copy(
                    pointsOfInterest = state.pointsOfInterest.copy(showManager = true, error = R.string.poi_limit_reached),
                ).toResult()
                else -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                    draft = PointDraft(event.latitude, event.longitude), selectedId = null, showManager = false, error = null,
                )).toResult()
            }
            is Event.OnPointClick -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                selectedId = event.id, showManager = false, error = null,
            ), selectedFriend = null).toResult()
            Event.DismissPointDetails -> if (state.pointsOfInterest.isSaving) state.toResult() else state.copy(
                pointsOfInterest = state.pointsOfInterest.copy(selectedId = null, deleteConfirmationId = null, error = null),
            ).toResult()
            Event.EditSelectedPoint -> {
                val point = state.pointsOfInterest.ownPoints.find { it.id == state.pointsOfInterest.selectedId }
                if (point == null || state.pointsOfInterest.isSaving) state.toResult() else state.copy(
                    pointsOfInterest = state.pointsOfInterest.copy(
                        draft = PointDraft(point.latitude, point.longitude, point.name, point.description, point.id),
                        selectedId = null, error = null,
                    ),
                ).toResult()
            }
            is Event.ChangePointName -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                draft = state.pointsOfInterest.draft?.copy(name = event.name), error = null,
            )).toResult()
            is Event.ChangePointDescription -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                draft = state.pointsOfInterest.draft?.copy(description = event.description), error = null,
            )).toResult()
            Event.DismissPointEditor -> if (state.pointsOfInterest.isSaving) state.toResult() else state.copy(
                pointsOfInterest = state.pointsOfInterest.copy(draft = null, error = null),
            ).toResult()
            Event.SavePoint -> savePoint(state)
            is Event.OnPointSaved -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                ownPoints = (state.pointsOfInterest.ownPoints.filterNot { it.id == event.point.id } + event.point)
                    .sortedByDescending { it.createdAt },
                draft = null, isSaving = false, showLayer = true, error = null,
            )).toResult()
            Event.RequestPointDeletion -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                deleteConfirmationId = state.pointsOfInterest.selectedId?.takeIf { id -> state.pointsOfInterest.ownPoints.any { it.id == id } },
                error = null,
            )).toResult()
            Event.CancelPointDeletion -> if (state.pointsOfInterest.isSaving) state.toResult() else state.copy(
                pointsOfInterest = state.pointsOfInterest.copy(deleteConfirmationId = null, error = null),
            ).toResult()
            Event.DeletePoint -> deletePoint(state)
            is Event.OnPointDeleted -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                ownPoints = state.pointsOfInterest.ownPoints.filterNot { it.id == event.id }, selectedId = null,
                deleteConfirmationId = null, isSaving = false, error = null,
                navigatingId = state.pointsOfInterest.navigatingId?.takeUnless { it == event.id },
            )).toResult()
            is Event.OnPointError -> state.copy(pointsOfInterest = state.pointsOfInterest.copy(
                isSaving = false, isLoading = false, error = event.message,
            )).toResult()
            is Event.NavigateToPoint -> state.copy(
                navigatingFriend = null, friendRoad = null, travelEstimate = null, interceptionPrediction = null,
                pointsOfInterest = state.pointsOfInterest.copy(navigatingId = event.id, selectedId = null, showManager = false),
            ).restartRoadWork()
        }
    }

    override fun State.toViewState(): ViewState {
        val now = currentTimeMillis()
        val displayUser = user?.let { roadDisplayLocation(it, userRoad, now) }
        val mappedUser = displayUser?.let { mapLocationUseCase.execute(it) }
        val mappedFriends = friends.map { friend ->
            val displayFriend = if (friend.username == navigatingFriend?.username) {
                friend.copy(location = friend.location?.let { roadDisplayLocation(it, friendRoad, now) })
            } else friend
            mapFriendUseCase.execute(displayFriend, user).copy(
                isPaused = pausedFriends.contains(friend.username)
            )
        }
        val currentFriend = mappedFriends.find { it.username == navigatingFriend?.username } ?: navigatingFriend
        val currentSelected = mappedFriends.find { it.username == selectedFriend?.username } ?: selectedFriend
        val allPoints = pointsOfInterest.ownPoints + friends.flatMap { it.pointsOfInterest }
        val selectedPoint = allPoints.find { it.id == pointsOfInterest.selectedId }
        val navigatingPoint = allPoints.find { it.id == pointsOfInterest.navigatingId }
        val pointBearing = if (mappedUser != null && navigatingPoint != null) {
            ((LocationUtils.calculateBearing(mappedUser.lat, mappedUser.lon, navigatingPoint.latitude, navigatingPoint.longitude) -
                mapSettings.bearing + 360) % 360).toFloat()
        } else null
        val pointDistance = if (user != null && navigatingPoint != null) {
            formatDistance(LocationUtils.calculateDistance(user.lat, user.lon, null, navigatingPoint.latitude, navigatingPoint.longitude, null))
        } else null
        val bearing = if (mappedUser != null && currentFriend != null) {
            currentFriend.location?.let { loc ->
                val geographicBearing = LocationUtils.calculateBearing(
                    mappedUser.lat,
                    mappedUser.lon,
                    loc.lat,
                    loc.lon
                )
                ((geographicBearing - mapSettings.bearing + 360) % 360).toFloat()
            }
        } else null

        val rawFriend = friends.find { it.username == currentFriend?.username }?.location
        val tracking = if (rawFriend?.isCarMode == true) {
            val fresh = rawFriend.isFresh(now) && user?.isFresh(now, 10_000L) == true
            val ownSpeed = user?.speedKmh()?.takeIf { fresh }
            val peerSpeed = rawFriend.speedKmh()?.takeIf { fresh }
            val estimate = travelEstimate?.takeIf { fresh && now - travelEstimateTime in 0..30_000L }
            CarTrackingInfo(
                friendSpeedKmh = peerSpeed,
                speedDifferenceKmh = if (ownSpeed != null && peerSpeed != null) ownSpeed - peerSpeed else null,
                travelSeconds = estimate?.durationSeconds?.takeUnless { isCarMode },
                routeDistanceMeters = estimate?.distanceMeters?.takeUnless { isCarMode },
                isLocationStale = !fresh,
                isPredictive = isCarMode,
                interceptionEstimate = if (isCarMode && fresh) interceptionPrediction?.currentEstimate(user, rawFriend, now) else null,
            )
        } else null

        return ViewState(
            mapSettings = mapSettings,
            user = mappedUser,
            friends = mappedFriends,
            selectedFriend = currentSelected,
            navigatingFriend = currentFriend,
            bearingToFriend = bearing,
            isCarMode = isCarMode,
            showCarSuggestion = showCarSuggestion,
            speedKmh = user?.speedKmh()?.takeIf { user.isFresh(now, 10_000L) },
            carTrackingInfo = tracking,
            mapDisplayLocation = displayUser?.takeIf { isMapVisible && isCarMode && it.isFresh(now, 10_000L) },
            pointsOfInterest = pointsOfInterest,
            points = allPoints,
            selectedPoint = selectedPoint,
            navigatingPoint = navigatingPoint,
            bearingToPoint = pointBearing,
            distanceToPoint = pointDistance,
        )
    }

    private fun savePoint(state: State): ReducerResult<State, Event, Action> {
        val points = state.pointsOfInterest
        val draft = points.draft ?: return state.toResult()
        if (points.isSaving || points.isLoading || !draft.isValid()) return state.toResult()
        if (draft.editingId == null && points.ownPoints.size >= MAX_POINTS_OF_INTEREST) {
            return state.copy(pointsOfInterest = points.copy(error = R.string.poi_limit_reached)).toResult()
        }
        return state.copy(pointsOfInterest = points.copy(isSaving = true, error = null)).toResult(SideEffect.AsyncWork {
            try {
                val point = if (draft.editingId == null) pointsOfInterestRepository.create(draft.request())
                    else pointsOfInterestRepository.update(draft.editingId, draft.request())
                Event.OnPointSaved(point)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Event.OnPointError(if (e is HttpException && e.code() == 409) R.string.poi_limit_reached else R.string.poi_save_error)
            }
        })
    }

    private fun deletePoint(state: State): ReducerResult<State, Event, Action> {
        val points = state.pointsOfInterest
        val id = points.deleteConfirmationId ?: return state.toResult()
        if (points.isSaving || points.isLoading || points.ownPoints.none { it.id == id }) return state.toResult()
        return state.copy(pointsOfInterest = points.copy(isSaving = true, error = null)).toResult(SideEffect.AsyncWork {
            try {
                pointsOfInterestRepository.delete(id)
                Event.OnPointDeleted(id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Event.OnPointError(R.string.poi_delete_error)
            }
        })
    }

    private suspend fun checkPaused(): Event {
        return try {
            when (val response = getPausedFriendsUseCase.execute()) {
                is SharingResponse.PausedFriends -> Event.OnPausedFriendsLoaded(response.usernames)
                is SharingResponse.ErrorData -> {
                    Timber.tag(TAG).d("Paused friends request rejected")
                    Event.NoOp
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Unable to load paused friends")
            Event.NoOp
        }
    }

    private fun roadWork() = SideEffect.AsyncWork<Event> {
        restartRoadUpdates()
        Event.NoOp
    }

    private fun State.restartRoadWork() = copy(roadVersion = roadVersion + 1).toResult(roadWork())

    private fun restartRoadUpdates() {
        roadUpdatesJob?.cancel()
        val currentDriver = currentState.friends.find { it.username == currentState.navigatingFriend?.username }?.location?.isCarMode == true
        if (!currentState.isMapVisible || (!currentState.isCarMode && !currentDriver)) return
        roadUpdatesJob = viewModelScope.launch {
            val userTrace = ArrayDeque<LocationData>()
            val friendTrace = ArrayDeque<LocationData>()
            var previousTarget: String? = null
            var lastRouteTime = 0L
            var lastUserMatchTime = 0L
            var lastFriendMatchTime = 0L
            var previousFreshness: Triple<Boolean, Boolean, Boolean>? = null
            while (isActive) {
                val snapshot = currentState
                val now = currentTimeMillis()
                val user = snapshot.user?.takeIf { it.isFresh(now, 10_000L) }
                val target = snapshot.navigatingFriend?.username
                val friend = snapshot.friends.find { it.username == target }?.location
                val freshness = Triple(snapshot.user?.isFresh(now, 10_000L) == true,
                    snapshot.user?.isFresh(now) == true, friend?.isFresh(now) == true)
                if (freshness != previousFreshness) {
                    previousFreshness = freshness
                    trigger(Event.CheckCarFreshness(now))
                }
                if (target != previousTarget) {
                    friendTrace.clear()
                    previousTarget = target
                    lastFriendMatchTime = 0L
                    lastRouteTime = 0L
                }
                fun ArrayDeque<LocationData>.addFix(fix: LocationData?) {
                    if (fix != null && fix.lastUpdate > (lastOrNull()?.lastUpdate ?: 0L)) {
                        addLast(fix)
                        while (size > 12) removeFirst()
                    }
                }
                userTrace.addFix(user)
                friendTrace.addFix(friend?.takeIf { it.isFresh(now) })
                val matchUser = snapshot.isCarMode && user != null && user.lastUpdate != lastUserMatchTime
                val matchFriend = friend?.isCarMode == true && friend.isFresh(now) && friend.lastUpdate != lastFriendMatchTime
                val route = friend?.isCarMode == true && friend.isFresh(now) && user != null && now - lastRouteTime >= 15_000L
                if (route) lastRouteTime = now
                if (matchUser) lastUserMatchTime = user.lastUpdate
                if (matchFriend) lastFriendMatchTime = friend.lastUpdate
                if (matchUser || matchFriend || route) {
                    try {
                        coroutineScope {
                            val userMatch = async { if (matchUser) roadNavigationRepository.matchRoad(userTrace.toList()) else null }
                            val friendMatch = async { if (matchFriend) roadNavigationRepository.matchRoad(friendTrace.toList()) else null }
                            val estimate = async { if (route && !snapshot.isCarMode) roadNavigationRepository.travelEstimate(user, friend) else null }
                            val interception = async {
                                if (route && snapshot.isCarMode) {
                                    val result = withTimeoutOrNull(8_000L) { roadNavigationRepository.interceptionEstimate(user, friend, now) }
                                        ?: RoadInterceptionEstimate.Unavailable
                                    CarInterceptionPrediction(result, user, friend, now)
                                } else null
                            }
                            trigger(Event.OnRoadUpdate(target, userMatch.await(), friendMatch.await(), estimate.await(), now, snapshot.roadVersion, interception.await()))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(TAG).w("Road data unavailable")
                    }
                }
                delay(2_000L)
            }
        }
    }

    sealed class Action {
        data class CenterMap(val zoom: Double) : Action()
        data class Zoom(val zoom: Double) : Action()
        data class Toast(@StringRes val id: Int) : Action()
    }

    sealed class Event {
        data object LoadOwnPoints : Event()
        data class OnOwnPointsLoaded(val points: List<PointOfInterest>) : Event()
        data object ShowPointManager : Event()
        data object DismissPointManager : Event()
        data class SetPointLayerVisible(val visible: Boolean) : Event()
        data class OnMapLongClick(val latitude: Double, val longitude: Double) : Event()
        data class OnPointClick(val id: String) : Event()
        data object DismissPointDetails : Event()
        data object EditSelectedPoint : Event()
        data class ChangePointName(val name: String) : Event()
        data class ChangePointDescription(val description: String) : Event()
        data object DismissPointEditor : Event()
        data object SavePoint : Event()
        data class OnPointSaved(val point: PointOfInterest) : Event()
        data object RequestPointDeletion : Event()
        data object CancelPointDeletion : Event()
        data object DeletePoint : Event()
        data class OnPointDeleted(val id: String) : Event()
        data class OnPointError(@param:StringRes val message: Int) : Event()
        data class NavigateToPoint(val id: String) : Event()
        data class OnLocationUpdate(val location: LocationData?) : Event()
        data class OnFriendsLoaded(val friends: List<FriendLocalData>) : Event()
        data class OnPausedFriendsLoaded(val pausedFriends: List<String>) : Event()
        data class ToggleSharing(val nick: String) : Event()
        data class OnActionSuccess(@StringRes val messageId: Int) : Event()
        data class OnError(@StringRes val id: Int) : Event()
        data object CheckPaused : Event()
        data object NoOp : Event()
        data object ZoomOut : Event()
        data object ZoomIn : Event()
        data object CenterMap : Event()
        data class OnFriendClick(val friend: Friend) : Event()
        data class OnFriendLongClick(val friend: Friend) : Event()
        data object DismissFriendDetails : Event()
        data class NavigateToFriend(val friend: Friend) : Event()
        data object CancelNavigation : Event()
        data class OnCameraUpdate(val bearing: Double) : Event()
        data class OnCarModeChanged(val enabled: Boolean) : Event()
        data class OnMapFpsLimitChanged(val limit: MapFpsLimit) : Event()
        data object AcceptCarSuggestion : Event()
        data object DismissCarSuggestion : Event()
        data class OnMapVisibilityChanged(val visible: Boolean) : Event()
        data class CheckCarFreshness(val time: Long) : Event()
        data class OnRoadUpdate(
            val target: String?,
            val userMatch: RoadMatch?,
            val friendMatch: RoadMatch?,
            val estimate: RoadTravelEstimate?,
            val estimatedAt: Long,
            val version: Long = 0L,
            val interception: CarInterceptionPrediction? = null,
        ) : Event()
    }

    data class State(
        val selectedFriend: Friend? = null,
        val navigatingFriend: Friend? = null,
        val mapSettings: MapSettings = MapSettings(),
        val friends: List<FriendLocalData> = emptyList(),
        val pausedFriends: List<String> = emptyList(),
        val user: LocationData? = null,
        val isCarMode: Boolean = false,
        val carModeLoaded: Boolean = false,
        val showCarSuggestion: Boolean = false,
        val suggestCarAfter: Long = 0L,
        val isMapVisible: Boolean = false,
        val userRoad: RoadMatch? = null,
        val friendRoad: RoadMatch? = null,
        val travelEstimate: RoadTravelEstimate? = null,
        val travelEstimateTime: Long = 0L,
        val interceptionPrediction: CarInterceptionPrediction? = null,
        val roadVersion: Long = 0L,
        // Re-evaluate stale readings only when a freshness boundary changes, without a UI frame timer.
        val freshnessCheckTime: Long = 0L,
        val pointsOfInterest: PointsOfInterestState = PointsOfInterestState(),
    )

    @Immutable
    data class ViewState(
        val selectedFriend: Friend?,
        val navigatingFriend: Friend?,
        val bearingToFriend: Float?,
        val mapSettings: MapSettings,
        val user: Location?,
        val friends: List<Friend>,
        val isCarMode: Boolean = false,
        val showCarSuggestion: Boolean = false,
        val speedKmh: Double? = null,
        val carTrackingInfo: CarTrackingInfo? = null,
        val mapDisplayLocation: LocationData? = null,
        val pointsOfInterest: PointsOfInterestState = PointsOfInterestState(),
        val points: List<PointOfInterest> = emptyList(),
        val selectedPoint: PointOfInterest? = null,
        val navigatingPoint: PointOfInterest? = null,
        val bearingToPoint: Float? = null,
        val distanceToPoint: String? = null,
    )

    companion object {
        private const val TAG = "MAP_VM"
    }

}
