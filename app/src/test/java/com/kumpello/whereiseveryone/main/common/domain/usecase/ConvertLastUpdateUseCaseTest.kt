package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertLastUpdateUseCaseTest {

    private val useCase = ConvertLastUpdateUseCase()

    @Test
    fun `execute returns FRESH for very recent updates`() {
        val now = 1000000000000L
        val lastUpdate = now - 30 * 1000L
        assertEquals(LastUpdateAge.FRESH, useCase.execute(lastUpdate, now))
    }

    @Test
    fun `execute returns OLD_AS_FUCK for very old updates`() {
        val now = 1000000000000L
        val lastUpdate = now - 120 * 60 * 1000L
        assertEquals(LastUpdateAge.OLD_AS_FUCK, useCase.execute(lastUpdate, now))
    }
}
