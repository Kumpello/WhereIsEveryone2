package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.kumpello.whereiseveryone.main.friends.model.FriendList
import com.squareup.moshi.Moshi
import androidx.core.content.edit

class SaveKeyUseCase(
    getEncryptedPreferencesUseCase: GetEncryptedPreferencesUseCase,
    private val getKeyUseCase: GetKeyUseCase,
) {

    private val moshi = Moshi.Builder().build()
    private val preferences = getEncryptedPreferencesUseCase.execute()

    fun saveUserID(id: String) {
        preferences.edit { putString(WhereIsEveryoneApplication.USER_ID_KEY, id) }
    }

    fun saveUserName(name: String) {
        preferences
            .edit {
                putString(WhereIsEveryoneApplication.USER_NAME_KEY, name)
            }
    }

    fun saveUserMessage(message: String) {
        preferences
            .edit {
                putString(WhereIsEveryoneApplication.USER_MESSAGE_KEY, message)
            }
    }

    fun saveAuthToken(token: String) {
        preferences
            .edit {
                putString(WhereIsEveryoneApplication.AUTH_TOKEN_KEY, token)
            }
    }

    fun saveAuthRefreshToken(token: String) {
        preferences
            .edit {
                putString(WhereIsEveryoneApplication.AUTH_REFRESH_TOKEN_KEY, token)
            }
    }

    fun saveLatitude(latitude: Double) {
        preferences
            .edit {
                putFloat(WhereIsEveryoneApplication.LATITUDE_KEY, latitude.toFloat())
            }
    }

    fun saveLongitude(longitude: Double) {
        preferences
            .edit {
                putFloat(WhereIsEveryoneApplication.LONGITUDE_KEY, longitude.toFloat())
            }
    }

    fun saveFriend(nick: String) {
        val currentList = getKeyUseCase.getFriends().toMutableList()
        currentList.add(Friend(nick))
        val adapter = moshi.adapter(FriendList::class.java)
        val jsonText: String = adapter.toJson(FriendList(currentList))
        preferences
            .edit {
                putString(WhereIsEveryoneApplication.FRIENDS_KEY, jsonText)
            }
    }
}