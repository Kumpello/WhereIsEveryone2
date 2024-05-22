package com.kumpello.whereiseveryone.main.friends.domain.model

import com.kumpello.whereiseveryone.common.model.ErrorData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface ObserveApi {

    @HTTP(method = "POST", path = "me/observe", hasBody = true)
    fun addFriend(@Header("Authorization") token:String, @Body nick: String): Call<ErrorData?>

    @HTTP(method = "DEL", path = "me/observe", hasBody = true)
    fun removeFriend(@Header("Authorization") token:String, @Body nick: String): Call<ErrorData?>

}
