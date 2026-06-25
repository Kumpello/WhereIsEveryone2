package com.kumpello.whereiseveryone.main.common.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class FormatLastUpdateUseCaseTest {

    private val useCase = FormatLastUpdateUseCase()

    @Test
    fun `execute returns formatted string`() {
        val instant = Instant.parse("2023-01-01T12:00:00Z")
        val result = useCase.execute(instant)
        // Check pattern HH:mm:ss dd.MM.yyyy
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}:\\d{2} \\d{2}\\.\\d{2}\\.\\d{4}")))
    }
}
