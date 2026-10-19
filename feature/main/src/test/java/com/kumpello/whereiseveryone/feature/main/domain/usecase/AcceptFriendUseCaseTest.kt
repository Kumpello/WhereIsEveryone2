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

class AcceptFriendUseCaseTest {

    private val friendRepository: FriendRepository = mockk()
    private val friendsStateRepository: FriendsStateRepository = mockk(relaxed = true)
    private val useCase = AcceptFriendUseCase(friendRepository, friendsStateRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { friendRepository.acceptFriendRequest("friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
        coVerify { friendsStateRepository.resolveIncomingFriendRequest("friend1") }
    }

    @Test
    fun `failed acceptance preserves the pending request`() = runTest {
        val error = CodeResponse.ErrorData(409, "Limit reached", "Limit reached")
        coEvery { friendRepository.acceptFriendRequest("friend1") } returns error

        assertEquals(error, useCase.execute("friend1"))
        coVerify(exactly = 0) { friendsStateRepository.resolveIncomingFriendRequest(any()) }
    }
}
