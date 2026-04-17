package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.model.FriendList
import com.squareup.moshi.Moshi

class SaveFriendsUseCase(
    private val getFriendsUseCase: GetFriendsUseCase,
    private val saveKeyUseCase: SaveKeyUseCase
) {

    private val moshi = Moshi.Builder().build()

    fun saveFriend(nick: String) {
        val currentList = getFriendsUseCase.getFriends().toMutableList()
        currentList.add(Friend(nick))
        val adapter = moshi.adapter(FriendList::class.java)
        val jsonText: String = adapter.toJson(FriendList(currentList))
        saveKeyUseCase.saveValue(WhereIsEveryoneApplication.FRIENDS_KEY, jsonText)
    }

}