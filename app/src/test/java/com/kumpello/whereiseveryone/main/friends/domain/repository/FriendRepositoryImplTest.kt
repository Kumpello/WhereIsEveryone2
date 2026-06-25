package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.FriendApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FriendRepositoryImplTest {

    private val friendApi: FriendApi = mockk()
    private val repository = FriendRepositoryImpl(friendApi)

    @Test
    fun `addFriend success returns SuccessNoContent`() = runTest {
        coEvery { friendApi.addFriend(any(), any()) } returns Response.success(null)

        val result = repository.addFriend("token", "user")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `addFriend failure returns ErrorData`() = runTest {
        coEvery { friendApi.addFriend(any(), any()) } returns Response.error(400, "Error".toResponseBody(null))

        val result = repository.addFriend("token", "user")

        assertTrue(result is CodeResponse.ErrorData)
    }

    @Test
    fun `removeFriend success returns SuccessNoContent`() = runTest {
        coEvery { friendApi.removeFriend(any(), any()) } returns Response.success(null)

        val result = repository.removeFriend("token", "user")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `acceptFriendRequest success returns SuccessNoContent`() = runTest {
        coEvery { friendApi.acceptFriendRequest(any(), any()) } returns Response.success(null)

        val result = repository.acceptFriendRequest("token", "user")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `rejectFriendRequest success returns SuccessNoContent`() = runTest {
        coEvery { friendApi.rejectFriendRequest(any(), any()) } returns Response.success(null)

        val result = repository.rejectFriendRequest("token", "user")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }
}
