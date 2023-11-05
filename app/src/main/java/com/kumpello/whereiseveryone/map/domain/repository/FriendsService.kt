package com.kumpello.whereiseveryone.map.domain.repository

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.friends.model.Friend
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class FriendsService @Inject constructor() {

    fun addFriend(application : WhereIsEveryoneApplication, nick : String) {
        application.addFriend(nick)
    }

    fun getFriends(application: WhereIsEveryoneApplication) : List<Friend> {
        return application.getFriends()
    }

}