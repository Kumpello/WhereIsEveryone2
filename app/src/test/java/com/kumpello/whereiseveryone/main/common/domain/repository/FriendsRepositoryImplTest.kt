package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FriendsRepositoryImplTest {

    private val friendsApi: FriendsApi = mockk()
    private val repository = FriendsRepositoryImpl(friendsApi)

    @Test
    fun `getFriends success returns FriendsData`() = runTest {
        coEvery { friendsApi.getFriends() } returns Response.success(emptyList())

        val result = repository.getFriends()

        assertTrue(result is FriendsResponse.FriendsData)
        assertEquals(0, (result as FriendsResponse.FriendsData).positions.size)
    }

    @Test
    fun `getFriends failure returns ErrorData`() = runTest {
        coEvery { friendsApi.getFriends() } returns Response.error(500, "Internal Server Error".toResponseBody(null))

        val result = repository.getFriends()

        assertTrue(result is FriendsResponse.ErrorData)
        assertEquals(500, (result as FriendsResponse.ErrorData).code)
    }
}
