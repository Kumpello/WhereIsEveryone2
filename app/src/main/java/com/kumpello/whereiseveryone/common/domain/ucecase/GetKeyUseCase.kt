package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.main.friends.model.Friend
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class GetKeyUseCase(
    private val getEncryptedPreferencesUseCase: GetEncryptedPreferencesUseCase,
) {

    private val moshi = Moshi.Builder().build()
    private val preferences = getEncryptedPreferencesUseCase.execute()

    fun getUserID(): String? {
        return preferences
            .getString(WhereIsEveryoneApplication.userIDKey, null)
    }

    fun getUserName(): String? {
        return preferences
            .getString(WhereIsEveryoneApplication.userNameKey, null)
    }

    fun getAuthToken(): String? {
        return preferences
            .getString(WhereIsEveryoneApplication.authTokenKey, null)
    }

    fun getAuthRefreshToken(): String? {
        return preferences
            .getString(WhereIsEveryoneApplication.authRefreshTokenKey, null)
    }

    fun getLatitude(): Double {
        return preferences
            .getFloat(WhereIsEveryoneApplication.latitudeKey, 0F)
            .toDouble()
    }

    fun getLongitude(): Double {
        return preferences
            .getFloat(WhereIsEveryoneApplication.longitudeKey, 0F)
            .toDouble()
    }

    fun getFriends(): List<Friend> {
        val defaultList = listOf<Friend>()
        val friendsList = Types.newParameterizedType(List::class.java, Friend::class.java)
        val adapter: JsonAdapter<List<Friend>> = moshi.adapter(friendsList)
        val jsonText = getEncryptedPreferencesUseCase
            .execute()
            .getString(
                WhereIsEveryoneApplication.friendsKey,
                defaultList.toString()
            )

        return jsonText?.let { adapter.fromJson(it) } ?: defaultList
    }

}