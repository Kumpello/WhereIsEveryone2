package com.kumpello.whereiseveryone.main.common.domain.usecase

import kotlin.math.*

class CalculateDistanceUseCase {
    /**
     * Calculates the distance between two points in 3D space in meters.
     */
    fun execute(
        lat1: Double, lon1: Double, alt1: Double?,
        lat2: Double, lon2: Double, alt2: Double?
    ): Double {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2).pow(2.0) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        val horizontalDistance = r * c
        val verticalDistance = (alt2 ?: 0.0) - (alt1 ?: 0.0)

        return sqrt(horizontalDistance.pow(2.0) + verticalDistance.pow(2.0))
    }
}
