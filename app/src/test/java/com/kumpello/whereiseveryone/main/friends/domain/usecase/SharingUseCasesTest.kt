package com.kumpello.whereiseveryone.main.friends.domain.usecase

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

    @Test
    fun `StopSharingUseCase execute returns data from repository`() = runTest {
        val useCase = StopSharingUseCase(sharingRepository)
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { sharingRepository.stopSharing("friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `ResumeSharingUseCase execute returns data from repository`() = runTest {
        val useCase = ResumeSharingUseCase(sharingRepository)
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { sharingRepository.resumeSharing("friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `GetPausedFriendsUseCase execute returns data from repository`() = runTest {
        val useCase = GetPausedFriendsUseCase(sharingRepository)
        val expectedResponse = SharingResponse.PausedFriends(listOf("friend1", "friend2"))
        coEvery { sharingRepository.getPausedFriends() } returns expectedResponse

        val result = useCase.execute()

        assertEquals(expectedResponse, result)
    }
}
