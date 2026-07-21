package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SharingUseCasesTest {

    private val sharingRepository: SharingRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    @Test
    fun `StopSharingUseCase execute returns data from repository`() = runTest {
        val useCase = StopSharingUseCase(sharingRepository, preferencesManager)
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { sharingRepository.stopSharing("token", "friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `ResumeSharingUseCase execute returns data from repository`() = runTest {
        val useCase = ResumeSharingUseCase(sharingRepository, preferencesManager)
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { sharingRepository.resumeSharing("token", "friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `GetPausedFriendsUseCase execute returns data from repository`() = runTest {
        val useCase = GetPausedFriendsUseCase(sharingRepository, preferencesManager)
        val expectedResponse = SharingResponse.PausedFriends(listOf("friend1", "friend2"))
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { sharingRepository.getPausedFriends("token") } returns expectedResponse

        val result = useCase.execute()

        assertEquals(expectedResponse, result)
    }
}
