package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SendLocationUseCaseTest {

    private val locationRepository: LocationRepository = mockk()
    private val useCase = SendLocationUseCase(locationRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        val lastUpdate = System.currentTimeMillis()
        coEvery { 
            locationRepository.sendPosition(1.0, 2.0, 0f, 3.0, 4f, 5f, lastUpdate) 
        } returns CodeResponse.SuccessNoContent

        val result = useCase.execute(1.0, 2.0, 0f, 3.0, 4f, 5f, lastUpdate)

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
