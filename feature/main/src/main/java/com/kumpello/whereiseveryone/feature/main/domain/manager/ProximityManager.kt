package com.kumpello.whereiseveryone.feature.main.domain.manager

import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.feature.main.ui.mapper.LocationUtils
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import com.kumpello.whereiseveryone.feature.main.location.LocationService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ProximityManager(
    private val locationService: LocationService,
    private val friendsManager: FriendsStateRepository,
    private val preferencesManager: PreferencesManager,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    suspend fun observeNearbyFriends(): Flow<List<String>> {
        return combine(
            locationService.observeLocation(),
            friendsManager.observeFriends(),
            preferencesManager.observe(PreferencesKey.ProximityDistance).map { it ?: 50 }
        ) { userLocation, friendsResponse, threshold ->
            if (userLocation == null || friendsResponse !is FriendsResponse.FriendsData) {
                return@combine emptyList<String>()
            }

            friendsResponse.positions.filter { friend ->
                val friendLoc = friend.location
                if (friendLoc != null) {
                    val distance = LocationUtils.calculateDistance(
                        userLocation.latitude, userLocation.longitude, userLocation.altitude,
                        friendLoc.latitude, friendLoc.longitude, friendLoc.altitude
                    )
                    distance <= threshold
                } else {
                    false
                }
            }.map { it.username }.sorted()
        }.distinctUntilChanged().flowOn(defaultDispatcher)
    }
}
