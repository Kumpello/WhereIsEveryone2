package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ConvertLastUpdateUseCaseTest {

    private val useCase = ConvertLastUpdateUseCase()

    @Test
    fun `execute returns FRESH for very recent updates`() {
        val now = Instant.parse("2023-01-01T12:00:00Z")
        val lastUpdate = now.minus(30.seconds)
        assertEquals(LastUpdateAge.FRESH, useCase.execute(lastUpdate, now))
    }

    @Test
    fun `execute returns OLD_AS_FUCK for very old updates`() {
        val now = Instant.parse("2023-01-01T12:00:00Z")
        val lastUpdate = now.minus(120.minutes)
        assertEquals(LastUpdateAge.OLD_AS_FUCK, useCase.execute(lastUpdate, now))
    }
}
