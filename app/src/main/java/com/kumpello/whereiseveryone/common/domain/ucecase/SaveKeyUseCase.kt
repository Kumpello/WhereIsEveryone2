package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.model.FriendList
import com.squareup.moshi.Moshi

class SaveKeyUseCase(
    getEncryptedPreferencesUseCase: GetEncryptedPreferencesUseCase,
    private val getKeyUseCase: GetKeyUseCase,
) {

    private val moshi = Moshi.Builder().build()
    private val preferences = getEncryptedPreferencesUseCase.execute()

    fun saveUserID(id: String) {
        preferences.edit().putString(WhereIsEveryoneApplication.userIDKey, id).apply()
    }

    fun saveUserName(name: String) {
        preferences
            .edit()
            .putString(WhereIsEveryoneApplication.userNameKey, name)
            .apply()
    }

    fun saveAuthToken(token: String) {
        preferences
            .edit()
            .putString(WhereIsEveryoneApplication.authTokenKey, token)
            .apply()
    }

    fun saveAuthRefreshToken(token: String) {
        preferences
            .edit()
            .putString(WhereIsEveryoneApplication.authRefreshTokenKey, token)
            .apply()
    }

    fun saveLatitude(latitude: Double) {
        preferences
            .edit()
            .putFloat(WhereIsEveryoneApplication.latitudeKey, latitude.toFloat())
            .apply()
    }

    fun saveLongitude(longitude: Double) {
        preferences
            .edit()
            .putFloat(WhereIsEveryoneApplication.longitudeKey, longitude.toFloat())
            .apply()
    }

    fun saveFriend(nick: String) {
        val currentList = getKeyUseCase.getFriends().toMutableList()
        currentList.add(Friend(nick, ""))
        val adapter = moshi.adapter(FriendList::class.java)
        val jsonText: String = adapter.toJson(FriendList(currentList))
        preferences
            .edit()
            .putString(WhereIsEveryoneApplication.friendsKey, jsonText)
            .apply()
    }
}