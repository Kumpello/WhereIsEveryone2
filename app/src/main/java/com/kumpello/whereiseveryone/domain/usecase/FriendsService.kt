package com.kumpello.whereiseveryone.domain.usecase

import android.content.Context
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.data.model.friends.Friend
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class FriendsService @Inject constructor() {

    fun addFriend(context : Context, nick : String) {
        val application = context.applicationContext as WhereIsEveryoneApplication
        application.addFriend(nick)
    }

    fun getFriends(context : Context) : List<Friend> {
        val application = context.applicationContext as WhereIsEveryoneApplication
        return application.getFriends()
    }

}