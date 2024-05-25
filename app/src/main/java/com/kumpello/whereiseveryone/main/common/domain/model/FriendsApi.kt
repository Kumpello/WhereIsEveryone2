package com.kumpello.whereiseveryone.main.common.domain.model

import com.kumpello.whereiseveryone.main.map.data.model.PositionsResponse
import retrofit2.Call
import retrofit2.http.HTTP
import retrofit2.http.Header

interface FriendsApi {
    @HTTP(method = "GET", path = "me/friends", hasBody = false)
    fun getPositions(@Header("Authorization") token:String): Call<PositionsResponse.FriendsData>
}