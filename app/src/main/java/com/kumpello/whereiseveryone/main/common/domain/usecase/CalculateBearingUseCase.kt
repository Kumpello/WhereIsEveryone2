package com.kumpello.whereiseveryone.main.common.domain.usecase

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CalculateBearingUseCase {
    /**
     * Calculates the bearing from point A to point B.
     * Result is in degrees (0..360).
     */
    fun execute(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360) % 360
        
        return bearing.toFloat()
    }
}
