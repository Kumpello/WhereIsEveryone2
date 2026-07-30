package com.kumpello.whereiseveryone.main.common.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.api.LocationApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class LocationRepositoryImplTest {

    private val locationApi: LocationApi = mockk()
    private val repository = LocationRepositoryImpl(locationApi)

    @Test
    fun `sendPosition success returns SuccessNoContent`() = runTest {
        coEvery { locationApi.sendLocation(any()) } returns Response.success(null)

        val result = repository.sendPosition(1.0, 2.0, 0f, 3.0, 4f, 5f, 1000L)

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `sendPosition failure returns ErrorData`() = runTest {
        coEvery { locationApi.sendLocation(any()) } returns Response.error(400, "Bad Request".toResponseBody(null))

        val result = repository.sendPosition(1.0, 2.0, 0f, 3.0, 4f, 5f, 1000L)

        assertTrue(result is CodeResponse.ErrorData)
        assertEquals(400, (result as CodeResponse.ErrorData).code)
    }

    @Test
    fun `wipeLocation success returns SuccessNoContent`() = runTest {
        coEvery { locationApi.wipeLocation() } returns Response.success(null)

        val result = repository.wipeLocation()

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `wipeLocation failure returns ErrorData`() = runTest {
        coEvery { locationApi.wipeLocation() } returns Response.error(500, "Server Error".toResponseBody(null))

        val result = repository.wipeLocation()

        assertTrue(result is CodeResponse.ErrorData)
        assertEquals(500, (result as CodeResponse.ErrorData).code)
    }
}
