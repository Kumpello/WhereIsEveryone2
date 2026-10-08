package com.kumpello.whereiseveryone.feature.main.ui.mapper

import com.kumpello.whereiseveryone.core.util.formatDistance
import com.kumpello.whereiseveryone.feature.main.ui.model.Friend
import com.kumpello.whereiseveryone.data.model.FriendLocalData
import com.kumpello.whereiseveryone.feature.main.ui.model.Location
import com.kumpello.whereiseveryone.data.model.LocationData


class MapFriendUseCase {
    fun execute(friend: FriendLocalData, userLocation: LocationData?): Friend {
        val friendLocation = friend.location
        val dist = if (userLocation != null && friendLocation != null) {
            LocationUtils.calculateDistance(
                userLocation.lat, userLocation.lon, userLocation.alt,
                friendLocation.lat, friendLocation.lon, friendLocation.alt
            )
        } else null
        return Friend(
            username = friend.username,
            status = friend.status,
            state = friend.state,
            location = friendLocation?.let { loc ->
                Location(
                    lat = loc.lat,
                    lon = loc.lon,
                    bearing = loc.bearing,
                    alt = LocationUtils.convertAlt(userLocation?.alt, loc.alt),
                    rawAlt = loc.alt,
                    accuracy = LocationUtils.convertAccuracy(loc.accuracy),
                    rawAccuracy = loc.accuracy,
                    speed = loc.speed,
                    lastUpdateTime = LocationUtils.formatLastUpdate(loc.lastUpdate),
                    lastUpdateAge = LocationUtils.convertLastUpdate(loc.lastUpdate)
                )
            },
            distance = dist,
            formattedDistance = dist?.let { formatDistance(it) },
            friendSince = friend.friendSince?.let { LocationUtils.formatDate(it) }
        )
    }
}
