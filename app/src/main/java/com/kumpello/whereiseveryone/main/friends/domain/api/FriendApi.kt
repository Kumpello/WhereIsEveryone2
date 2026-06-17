package com.kumpello.whereiseveryone.main.friends.domain.api


import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface FriendApi {

    @HTTP(method = "POST", path = "me/friend", hasBody = true)
    suspend fun addFriend(@Header("Authorization") token:String, @Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "DEL", path = "me/friend", hasBody = true)
    suspend fun removeFriend(@Header("Authorization") token:String, @Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/friend/accept", hasBody = true)
    suspend fun acceptFriendRequest(@Header("Authorization") token:String, @Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/friend/reject", hasBody = true)
    suspend fun rejectFriendRequest(@Header("Authorization") token:String, @Body request: FriendRequest): Response<ResponseBody>

}
