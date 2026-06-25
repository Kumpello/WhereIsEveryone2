package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.common.domain.ucecase.GetCurrentAuthTokenUseCase
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFriendsDataUseCaseTest {

    private val friendsRepository: FriendsRepository = mockk()
    private val getCurrentAuthTokenUseCase: GetCurrentAuthTokenUseCase = mockk()
    private val useCase = GetFriendsDataUseCase(friendsRepository, getCurrentAuthTokenUseCase)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = FriendsResponse.FriendsData(emptyList())
        coEvery { getCurrentAuthTokenUseCase.execute() } returns "token"
        coEvery { friendsRepository.getFriends("token") } returns expectedResponse

        val result = useCase.execute()

        assertEquals(expectedResponse, result)
    }
}
