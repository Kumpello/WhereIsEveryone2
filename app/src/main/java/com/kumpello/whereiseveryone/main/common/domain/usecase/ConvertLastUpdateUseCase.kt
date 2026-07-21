package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge

class ConvertLastUpdateUseCase {

    fun execute(lastUpdate: Long, now: Long = System.currentTimeMillis()): LastUpdateAge {
        val duration = now - lastUpdate
        
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
        const val FRESH_LIMIT = 60 * 1000L
        const val NEW_LIMIT = 5 * 60 * 1000L
        const val SOMEWHAT_NEW_LIMIT = 15 * 60 * 1000L
        const val SOMEWHAT_OLD_LIMIT = 30 * 60 * 1000L
        const val OLD_LIMIT = 60 * 60 * 1000L
    }
}
