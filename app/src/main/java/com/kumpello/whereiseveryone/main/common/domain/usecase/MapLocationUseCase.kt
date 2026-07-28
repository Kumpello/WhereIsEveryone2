package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.entity.LocationData

import com.kumpello.whereiseveryone.main.common.util.LocationUtils

class MapLocationUseCase {
    fun execute(data: LocationData): Location {
        return Location(
            lat = data.lat,
            lon = data.lon,
            bearing = data.bearing,
            alt = AltDifference.SOMEWHAT_SAME,
            accuracy = LocationUtils.convertAccuracy(data.accuracy),
            lastUpdateTime = LocationUtils.formatLastUpdate(data.lastUpdate),
            lastUpdateAge = LocationUtils.convertLastUpdate(data.lastUpdate),
            rawAlt = data.alt,
            rawAccuracy = data.accuracy,
            speed = data.speed
        )
    }
}
