package com.kumpello.whereiseveryone.main.friends.domain.api

import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface SharingApi {

    @HTTP(method = "POST", path = "me/sharing/stop", hasBody = true)
    suspend fun stopSharing(@Header("Authorization") token: String, @Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/sharing/resume", hasBody = true)
    suspend fun resumeSharing(@Header("Authorization") token: String, @Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "GET", path = "me/sharing", hasBody = false)
    suspend fun getPaused(@Header("Authorization") token: String): Response<List<String>>
}
