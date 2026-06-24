package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.Location
import com.kumpello.whereiseveryone.main.common.entity.LocationData

class MapLocationUseCase(
    private val convertAccuracyUseCase: ConvertAccuracyUseCase,
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase,
    private val formatLastUpdateUseCase: FormatLastUpdateUseCase
) {
    fun execute(data: LocationData): Location {
        return Location(
            lat = data.lat,
            lon = data.lon,
            bearing = data.bearing,
            alt = AltDifference.SOMEWHAT_SAME,
            accuracy = convertAccuracyUseCase.execute(data.accuracy),
            lastUpdateTime = formatLastUpdateUseCase.execute(data.last_update),
            lastUpdateAge = convertLastUpdateUseCase.execute(data.last_update),
            rawAlt = 0.0,
            rawAccuracy = 0.0f
        )
    }
}
