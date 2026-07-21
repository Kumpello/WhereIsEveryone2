package com.kumpello.whereiseveryone.main.map.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateStatusUseCaseTest {

    private val statusRepository: StatusRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val useCase = UpdateStatusUseCase(statusRepository, preferencesManager)

    @Test
    fun `execute returns data from repository`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { statusRepository.updateStatus("token", "busy") } returns CodeResponse.SuccessNoContent

        val result = useCase.execute("busy")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
