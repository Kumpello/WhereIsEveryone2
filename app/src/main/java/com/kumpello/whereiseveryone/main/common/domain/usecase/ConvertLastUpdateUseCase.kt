package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ConvertLastUpdateUseCase {

    fun execute(lastUpdate: Instant, now: Instant = Clock.System.now()): LastUpdateAge {
        val duration = now.minus(lastUpdate)
        
        return when {
            duration < FRESH_LIMIT -> LastUpdateAge.FRESH
            duration < NEW_LIMIT -> LastUpdateAge.NEW
            duration < SOMEWHAT_NEW_LIMIT -> LastUpdateAge.SOMEWHAT_NEW
            duration < SOMEWHAT_OLD_LIMIT -> LastUpdateAge.SOMEWHAT_OLD
            duration < OLD_LIMIT -> LastUpdateAge.OLD
            else -> LastUpdateAge.OLD_AS_FUCK
        }
    }

    companion object {
        val FRESH_LIMIT = 1.minutes
        val NEW_LIMIT = 5.minutes
        val SOMEWHAT_NEW_LIMIT = 15.minutes
        val SOMEWHAT_OLD_LIMIT = 30.minutes
        val OLD_LIMIT = 60.minutes
    }
}
