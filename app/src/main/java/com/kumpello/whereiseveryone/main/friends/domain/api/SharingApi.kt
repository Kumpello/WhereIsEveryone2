package com.kumpello.whereiseveryone.main.friends.domain.api

import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP

interface SharingApi {

    @HTTP(method = "POST", path = "me/sharing/stop", hasBody = true)
    suspend fun stopSharing(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/sharing/resume", hasBody = true)
    suspend fun resumeSharing(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "GET", path = "me/sharing", hasBody = false)
    suspend fun getPaused(): Response<List<String>>
}
