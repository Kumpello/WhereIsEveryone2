package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.model.CodeResponse
import com.kumpello.whereiseveryone.data.repository.FriendRepository
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RejectFriendUseCaseTest {

    private val friendRepository: FriendRepository = mockk()
    private val friendsStateRepository: FriendsStateRepository = mockk(relaxed = true)
    private val useCase = RejectFriendUseCase(friendRepository, friendsStateRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { friendRepository.rejectFriendRequest("friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
        coVerify { friendsStateRepository.resolveIncomingFriendRequest("friend1") }
    }

    @Test
    fun `failed rejection preserves the pending request`() = runTest {
        val error = CodeResponse.ErrorData(503, "Unavailable", "Unavailable")
        coEvery { friendRepository.rejectFriendRequest("friend1") } returns error

        assertEquals(error, useCase.execute("friend1"))
        coVerify(exactly = 0) { friendsStateRepository.resolveIncomingFriendRequest(any()) }
    }
}
