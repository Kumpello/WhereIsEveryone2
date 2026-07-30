package com.kumpello.whereiseveryone.main.map.domain.repository

import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import com.kumpello.whereiseveryone.main.map.domain.api.StatusApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class StatusRepositoryImplTest {

    private val statusApi: StatusApi = mockk()
    private val repository = StatusRepositoryImpl(statusApi)

    @Test
    fun `updateStatus success returns SuccessNoContent`() = runTest {
        coEvery { statusApi.updateStatus(any()) } returns Response.success(null)

        val result = repository.updateStatus("Feeling good")

        assertEquals(CodeResponse.SuccessNoContent, result)
    }

    @Test
    fun `updateStatus failure returns ErrorData`() = runTest {
        coEvery { statusApi.updateStatus(any()) } returns Response.error(500, "Error".toResponseBody(null))

        val result = repository.updateStatus("Feeling bad")

        assertTrue(result is CodeResponse.ErrorData)
        assertEquals(500, (result as CodeResponse.ErrorData).code)
    }
}
