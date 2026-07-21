package com.kumpello.whereiseveryone.main.common.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class FormatLastUpdateUseCaseTest {

    private val useCase = FormatLastUpdateUseCase()

    @Test
    fun `execute returns formatted string`() {
        val timestamp = 1672574400000L // 2023-01-01T12:00:00Z
        val result = useCase.execute(timestamp)
        // Check pattern HH:mm:ss dd.MM.yyyy
        assertTrue(result.matches(Regex("\\d{2}:\\d{2}:\\d{2} \\d{2}\\.\\d{2}\\.\\d{4}")))
    }
}
