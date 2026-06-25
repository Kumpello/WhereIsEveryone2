package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStatusUseCaseTest {

    private val statusRepository: StatusRepository = mockk()
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase = mockk()
    private val useCase = UpdateStatusUseCase(statusRepository, getCurrentAuthTokenUseCase)

    @Test
    fun `execute returns data from repository`() = runTest {
        coEvery { getCurrentAuthTokenUseCase.execute() } returns "token"
        coEvery { statusRepository.updateStatus("token", "busy") } returns CodeResponse.SuccessNoContent

        val result = useCase.execute("busy")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
