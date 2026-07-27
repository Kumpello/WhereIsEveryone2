package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AcceptFriendUseCaseTest {

    private val friendRepository: FriendRepository = mockk()
    private val useCase = AcceptFriendUseCase(friendRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { friendRepository.acceptFriendRequest("friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
    }
}
