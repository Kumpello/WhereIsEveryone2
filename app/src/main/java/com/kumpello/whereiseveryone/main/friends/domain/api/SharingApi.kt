package com.kumpello.whereiseveryone.main.friends.domain.api

import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import com.kumpello.whereiseveryone.main.friends.domain.model.SharingResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SharingApi {

    @POST("sharing/stop")
    suspend fun stopSharing(@Header("Authorization") token: String, @Body request: FriendRequest): Response<ResponseBody>

    @POST("sharing/resume")
    suspend fun resumeSharing(@Header("Authorization") token: String, @Body request: FriendRequest): Response<ResponseBody>

    @GET("sharing")
    suspend fun getPaused(@Header("Authorization") token: String): Response<SharingResponse.PausedFriends>
}
