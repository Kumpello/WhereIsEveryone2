package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.repository.FriendsRepository
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFriendsDataUseCaseTest {

    private val friendsRepository: FriendsRepository = mockk()
    private val useCase = GetFriendsDataUseCase(friendsRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = FriendsResponse.FriendsData(emptyList())
        coEvery { friendsRepository.getFriends() } returns expectedResponse

        val result = useCase.execute()

        assertEquals(expectedResponse, result)
    }
}
