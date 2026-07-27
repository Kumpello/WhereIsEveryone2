package com.kumpello.whereiseveryone.main.friends.domain.api


import com.kumpello.whereiseveryone.main.friends.domain.model.FriendRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP

interface FriendApi {

    @HTTP(method = "POST", path = "me/friend", hasBody = true)
    suspend fun addFriend(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "me/friend", hasBody = true)
    suspend fun removeFriend(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/friend/accept", hasBody = true)
    suspend fun acceptFriendRequest(@Body request: FriendRequest): Response<ResponseBody>

    @HTTP(method = "POST", path = "me/friend/reject", hasBody = true)
    suspend fun rejectFriendRequest(@Body request: FriendRequest): Response<ResponseBody>

}
