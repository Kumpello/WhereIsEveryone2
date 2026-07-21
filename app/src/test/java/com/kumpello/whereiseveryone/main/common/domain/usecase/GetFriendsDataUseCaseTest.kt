package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFriendsDataUseCaseTest {

    private val friendsRepository: FriendsRepository = mockk()
    private val preferencesManager: PreferencesManager = mockk()
    private val useCase = GetFriendsDataUseCase(friendsRepository, preferencesManager)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = FriendsResponse.FriendsData(emptyList())
        coEvery { preferencesManager.get(PreferencesKey.AuthToken) } returns "token"
        coEvery { friendsRepository.getFriends("token") } returns expectedResponse

        val result = useCase.execute()

        assertEquals(expectedResponse, result)
    }
}
