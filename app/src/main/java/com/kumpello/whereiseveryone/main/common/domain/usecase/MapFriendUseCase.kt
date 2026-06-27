package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.extension.formatDistance
import com.kumpello.whereiseveryone.main.common.entity.Friend
import com.kumpello.whereiseveryone.main.common.entity.FriendLocalData
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.entity.LocationData

class MapFriendUseCase(
    private val calculateDistanceUseCase: CalculateDistanceUseCase,
    private val convertAltUseCase: ConvertAltUseCase,
    private val convertAccuracyUseCase: ConvertAccuracyUseCase,
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase,
    private val formatLastUpdateUseCase: FormatLastUpdateUseCase,
    private val formatDateUseCase: FormatDateUseCase
) {
    fun execute(friend: FriendLocalData, userLocation: LocationData?): Friend {
        val friendLocation = friend.location
        val dist = if (userLocation != null && friendLocation != null) {
            calculateDistanceUseCase.execute(
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
                    alt = convertAltUseCase.execute(userLocation?.alt, loc.alt),
                    rawAlt = loc.alt,
                    accuracy = convertAccuracyUseCase.execute(loc.accuracy),
                    rawAccuracy = loc.accuracy,
                    lastUpdateTime = formatLastUpdateUseCase.execute(loc.last_update),
                    lastUpdateAge = convertLastUpdateUseCase.execute(loc.last_update)
                )
            },
            distance = dist,
            formattedDistance = dist?.let { formatDistance(it) },
            friendSince = friend.friendSince?.let { formatDateUseCase.execute(it) }
        )
    }
}
