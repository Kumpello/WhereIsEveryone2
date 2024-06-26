package com.kumpello.whereiseveryone.main.friends.domain.model


import com.kumpello.whereiseveryone.common.domain.model.CodeResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header

interface ObserveApi {

    @HTTP(method = "POST", path = "me/observe", hasBody = true)
    fun addFriend(@Header("Authorization") token:String, @Body nick: String): Call<CodeResponse.SuccessNoContent?>

    @HTTP(method = "DEL", path = "me/observe", hasBody = true)
    fun removeFriend(@Header("Authorization") token:String, @Body nick: String): Call<CodeResponse.SuccessNoContent>

}
