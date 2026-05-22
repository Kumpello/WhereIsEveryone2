package com.kumpello.whereiseveryone.main.common.domain.model

import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import retrofit2.Call
import retrofit2.http.HTTP
import retrofit2.http.Header

interface FriendsApi {
    @HTTP(method = "GET", path = "me/friends", hasBody = false)
    fun getFriends(@Header("Authorization") token:String): Call<List<FriendData>>
}