package com.kumpello.whereiseveryone.main.friends.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddFriendUseCaseTest {

    private val friendRepository: FriendRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val useCase = AddFriendUseCase(friendRepository, preferencesManager)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = CodeResponse.SuccessNoContent
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { friendRepository.addFriend("token", "friend1") } returns expectedResponse

        val result = useCase.execute("friend1")

        assertEquals(expectedResponse, result)
    }
}
