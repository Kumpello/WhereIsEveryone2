package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WipeLocationUseCaseTest {

    private val locationRepository: LocationRepository = mockk()
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase = mockk()
    private val useCase = WipeLocationUseCase(locationRepository, getCurrentAuthTokenUseCase)

    @Test
    fun `execute returns data from repository`() = runTest {
        coEvery { getCurrentAuthTokenUseCase.execute() } returns "token"
        coEvery { locationRepository.wipeLocation("token") } returns CodeResponse.SuccessNoContent

        val result = useCase.execute()

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
