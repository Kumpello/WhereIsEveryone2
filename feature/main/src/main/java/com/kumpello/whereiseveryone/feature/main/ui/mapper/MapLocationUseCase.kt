package com.kumpello.whereiseveryone.feature.main.ui.mapper

import com.kumpello.whereiseveryone.feature.main.ui.model.AltDifference
import com.kumpello.whereiseveryone.feature.main.ui.model.Location
import com.kumpello.whereiseveryone.data.model.LocationData


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
