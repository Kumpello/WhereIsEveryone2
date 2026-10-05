package com.kumpello.whereiseveryone.main.common.domain.model

import com.kumpello.whereiseveryone.main.map.domain.model.FriendsPage
import retrofit2.Response
import retrofit2.http.HTTP
import retrofit2.http.Query

interface FriendsApi {
    @HTTP(method = "GET", path = "me/friends", hasBody = false)
    suspend fun getFriends(
        @Query("state") state: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<FriendsPage>
}
