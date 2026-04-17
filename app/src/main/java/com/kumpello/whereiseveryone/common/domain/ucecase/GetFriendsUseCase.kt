package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class GetFriendsUseCase(private val getKeyUseCase: GetKeyUseCase) {

    private val moshi = Moshi.Builder().build()

    fun getFriends(): List<Friend> {
        val defaultList = listOf<Friend>()
        val friendsList = Types.newParameterizedType(List::class.java, Friend::class.java)
        val adapter: JsonAdapter<List<Friend>> = moshi.adapter(friendsList)
        val jsonText = getKeyUseCase.getValue(WhereIsEveryoneApplication.FRIENDS_KEY)

        return jsonText?.let { adapter.fromJson(it) } ?: defaultList
    }

}