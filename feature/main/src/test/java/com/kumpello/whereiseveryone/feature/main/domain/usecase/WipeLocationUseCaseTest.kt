package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WipeLocationUseCaseTest {

    private val locationRepository: LocationRepository = mockk()
    private val useCase = WipeLocationUseCase(locationRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        coEvery { locationRepository.wipeLocation() } returns CodeResponse.SuccessNoContent

        val result = useCase.execute()

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
