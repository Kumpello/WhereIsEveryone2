package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WipeLocationUseCaseTest {

    private val locationRepository: LocationRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val useCase = WipeLocationUseCase(locationRepository, preferencesManager)

    @Test
    fun `execute returns data from repository`() = runTest {
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { locationRepository.wipeLocation("token") } returns CodeResponse.SuccessNoContent

        val result = useCase.execute()

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
