package com.kumpello.whereiseveryone.main.common.domain.model

import com.kumpello.whereiseveryone.main.map.domain.model.FriendData
import retrofit2.Response
import retrofit2.http.HTTP

interface FriendsApi {
    @HTTP(method = "GET", path = "me/friends", hasBody = false)
    suspend fun getFriends(): Response<List<FriendData>>
}