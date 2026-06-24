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
    private val formatLastUpdateUseCase: FormatLastUpdateUseCase
) {
    fun execute(friend: FriendLocalData, userLocation: LocationData?): Friend {
        val dist = userLocation?.let {
            calculateDistanceUseCase.execute(
                it.lat, it.lon, it.alt,
                friend.location.lat, friend.location.lon, friend.location.alt
            )
        }
        return Friend(
            username = friend.username,
            status = friend.status,
            state = friend.state,
            location = Location(
                lat = friend.location.lat,
                lon = friend.location.lon,
                bearing = friend.location.bearing,
                alt = convertAltUseCase.execute(userLocation?.alt, friend.location.alt),
                rawAlt = friend.location.alt,
                accuracy = convertAccuracyUseCase.execute(friend.location.accuracy),
                rawAccuracy = friend.location.accuracy,
                lastUpdateTime = formatLastUpdateUseCase.execute(friend.location.last_update),
                lastUpdateAge = convertLastUpdateUseCase.execute(friend.location.last_update)
            ),
            distance = dist,
            formattedDistance = dist?.let { formatDistance(it) }
        )
    }
}
