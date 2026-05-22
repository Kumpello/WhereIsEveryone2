package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AltDifference

class ConvertAltUseCase {
    fun execute(userAlt: Double?, friendAlt: Double?): AltDifference {
        if (userAlt == null || friendAlt == null) return AltDifference.SOMEWHAT_SAME
        val difference = friendAlt.minus(userAlt)
        return when {
            difference < -DIFFERENCE_LIMIT -> AltDifference.WAY_LOWER
            difference > DIFFERENCE_LIMIT -> AltDifference.WAY_HIGHER
            else -> AltDifference.SOMEWHAT_SAME
        }
    }

    companion object {
        const val DIFFERENCE_LIMIT: Double = 50.0
    }
}