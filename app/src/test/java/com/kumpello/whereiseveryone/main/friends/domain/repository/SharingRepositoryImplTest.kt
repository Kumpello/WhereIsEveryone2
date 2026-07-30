package com.kumpello.whereiseveryone.main.friends.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.friends.domain.api.SharingApi
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SharingRepositoryImplTest {

    private val sharingApi: SharingApi = mockk()
    private val repository = SharingRepositoryImpl(sharingApi)

    @Test
    fun `stopSharing success returns SuccessNoContent`() = runTest {
        coEvery { sharingApi.stopSharing(any()) } returns Response.success(null)

        val result = repository.stopSharing("friend1")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `stopSharing failure returns ErrorData`() = runTest {
        coEvery { sharingApi.stopSharing(any()) } returns Response.error(400, "Bad Request".toResponseBody(null))

        val result = repository.stopSharing("friend1")

        assertTrue(result is CodeResponse.ErrorData)
        assertEquals(400, (result as CodeResponse.ErrorData).code)
    }

    @Test
    fun `resumeSharing success returns SuccessNoContent`() = runTest {
        coEvery { sharingApi.resumeSharing(any()) } returns Response.success(null)

        val result = repository.resumeSharing("friend1")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `getPausedFriends success returns PausedFriends`() = runTest {
        val paused = listOf("friend1", "friend2")
        coEvery { sharingApi.getPaused() } returns Response.success(paused)

        val result = repository.getPausedFriends()

        assertTrue(result is SharingResponse.PausedFriends)
        assertEquals(paused, (result as SharingResponse.PausedFriends).usernames)
    }

    @Test
    fun `getPausedFriends failure returns ErrorData`() = runTest {
        coEvery { sharingApi.getPaused() } returns Response.error(500, "Server Error".toResponseBody(null))

        val result = repository.getPausedFriends()

        assertTrue(result is SharingResponse.ErrorData)
        assertEquals(500, (result as SharingResponse.ErrorData).code)
    }
}
