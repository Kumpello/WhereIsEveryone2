package com.kumpello.whereiseveryone.data.network.api


import com.kumpello.whereiseveryone.data.network.model.FriendRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP

internal interface FriendApi {

    @HTTP(method = "POST", path = "me/friend", hasBody = true)
    suspend fun addFriend(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "me/friend", hasBody = true)
    suspend fun removeFriend(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/friend/accept", hasBody = true)
    suspend fun acceptFriendRequest(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/friend/reject", hasBody = true)
    suspend fun rejectFriendRequest(@Body request: FriendRequest): Response<ResponseBody>

}
