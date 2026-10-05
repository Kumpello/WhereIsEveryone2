package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsPage
import com.kumpello.whereiseveryone.main.map.domain.model.FriendsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class FriendsRepositoryImplTest {
    private val friendsApi: FriendsApi = mockk()
    private val repository = FriendsRepositoryImpl(friendsApi)

    @Before
    fun setup() {
        coEvery { friendsApi.getFriends(any(), any(), any()) } returns Response.success(FriendsPage(emptyList(), null))
    }

    private fun friend(name: String, state: String = "accepted") = FriendData(name, "", state, null, null)

    @Test
    fun `all pages and relationship groups are loaded before returning success`() = runTest {
        coEvery { friendsApi.getFriends("accepted", null, 50) } returns
            Response.success(FriendsPage(listOf(friend("alice")), "accepted-next"))
        coEvery { friendsApi.getFriends("accepted", "accepted-next", 50) } returns
            Response.success(FriendsPage(listOf(friend("bob")), null))
        coEvery { friendsApi.getFriends("pending_incoming", null, 50) } returns
            Response.success(FriendsPage(listOf(friend("incoming", "pending_incoming")), "incoming-next"))
        coEvery { friendsApi.getFriends("pending_incoming", "incoming-next", 50) } returns
            Response.success(FriendsPage(emptyList(), null))
        coEvery { friendsApi.getFriends("pending_outgoing", null, 50) } returns
            Response.success(FriendsPage(listOf(friend("outgoing", "pending_outgoing")), null))

        val result = repository.getFriends() as FriendsResponse.FriendsData

        assertEquals(listOf("alice", "bob", "incoming", "outgoing"), result.positions.map { it.username })
        coVerify(exactly = 5) { friendsApi.getFriends(any(), any(), 50) }
    }

    @Test
    fun `empty groups return a complete empty list`() = runTest {
        val result = repository.getFriends() as FriendsResponse.FriendsData
        assertTrue(result.positions.isEmpty())
        coVerify(exactly = 3) { friendsApi.getFriends(any(), null, 50) }
    }

    @Test
    fun `later page failure returns an error without publishing a partial list`() = runTest {
        coEvery { friendsApi.getFriends("accepted", null, 50) } returns
            Response.success(FriendsPage(listOf(friend("alice")), "next"))
        coEvery { friendsApi.getFriends("accepted", "next", 50) } returns
            Response.error(500, "Internal Server Error".toResponseBody(null))

        val result = repository.getFriends() as FriendsResponse.ErrorData

        assertEquals(500, result.code)
        coVerify(exactly = 0) { friendsApi.getFriends("pending_incoming", any(), any()) }
    }

    @Test
    fun `duplicate usernames across pages and lists preserve accepted relationship`() = runTest {
        coEvery { friendsApi.getFriends("accepted", null, 50) } returns
            Response.success(FriendsPage(listOf(friend("alice")), "next"))
        coEvery { friendsApi.getFriends("accepted", "next", 50) } returns
            Response.success(FriendsPage(listOf(friend("alice")), null))
        coEvery { friendsApi.getFriends("pending_incoming", null, 50) } returns
            Response.success(FriendsPage(listOf(friend("alice", "pending_incoming")), null))

        val result = repository.getFriends() as FriendsResponse.FriendsData

        assertEquals(listOf(friend("alice")), result.positions)
    }

    @Test
    fun `a repeated cursor stops traversal instead of looping`() = runTest {
        coEvery { friendsApi.getFriends("accepted", any(), 50) } returns
            Response.success(FriendsPage(emptyList(), "same"))
        try {
            repository.getFriends()
            fail("Expected IOException")
        } catch (_: IOException) {
            coVerify(exactly = 2) { friendsApi.getFriends(any(), any(), any()) }
        }
    }

    @Test
    fun `malformed pages fail without returning partial data`() = runTest {
        for (page in listOf(
            FriendsPage(List(51) { friend("peer-$it") }, null),
            FriendsPage(listOf(friend("peer", "pending_incoming")), null),
            FriendsPage(emptyList(), " ")
        )) {
            coEvery { friendsApi.getFriends("accepted", null, 50) } returns Response.success(page)
            try {
                repository.getFriends()
                fail("Expected IOException")
            } catch (_: IOException) {
                // No successful FriendsData can reach the cache.
            }
        }
    }

    @Test
    fun `network errors and cancellation propagate from any page`() = runTest {
        for (failure in listOf(IOException("offline"), CancellationException("cancelled"))) {
            coEvery { friendsApi.getFriends("accepted", null, 50) } throws failure
            try {
                repository.getFriends()
                fail("Expected failure")
            } catch (caught: Exception) {
                assertEquals(failure, caught)
            }
        }
    }
}
