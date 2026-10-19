package com.kumpello.whereiseveryone.feature.main.domain.usecase

import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFriendsDataUseCaseTest {

    private val friendsRepository: FriendsStateRepository = mockk()
    private val useCase = GetFriendsDataUseCase(friendsRepository)

    @Test
    fun `execute returns data from repository`() = runTest {
        val expectedResponse = FriendsResponse.FriendsData(emptyList())
        coEvery { friendsRepository.refresh() } returns expectedResponse

        val result = useCase.execute()

        assertEquals(expectedResponse, result)
    }
}
