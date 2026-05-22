package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel

class ConvertAccuracyUseCase {
    fun execute(accuracy: Float?) : AccuracyLevel {
        if (accuracy == null) return AccuracyLevel.PERFECT //TODO Add unknown
        return when {
            accuracy > TRAGIC_LIMIT -> AccuracyLevel.TRAGIC
            accuracy > LOW_LIMIT -> AccuracyLevel.LOW
            accuracy > MEDIUM_LIMIT -> AccuracyLevel.MEDIUM
            accuracy > HIGH_LIMIT -> AccuracyLevel.HIGH
            else -> AccuracyLevel.PERFECT
        }
    }

    companion object {
        const val TRAGIC_LIMIT: Float = 30f
        const val LOW_LIMIT: Float = 15f
        const val MEDIUM_LIMIT: Float = 7.5f
        const val HIGH_LIMIT: Float = 3f
    }
}