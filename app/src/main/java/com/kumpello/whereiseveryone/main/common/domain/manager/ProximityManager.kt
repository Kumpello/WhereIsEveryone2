package com.kumpello.whereiseveryone.main.common.domain.manager

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.common.util.LocationUtils
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import com.kumpello.whereiseveryone.main.map.presentation.LocationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ProximityManager(
    private val locationService: LocationService,
    private val friendsManager: FriendsManager,
    private val preferencesManager: PreferencesManager
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
        }.distinctUntilChanged()
    }
}
