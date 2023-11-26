package com.kumpello.whereiseveryone.app

import android.app.Application


class WhereIsEveryoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger()
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }

    companion object {

        const val preferencesName = "secret_keeper"
        const val userIDKey = "user_id"
        const val userNameKey = "user_name"
        const val authTokenKey = "auth_token"
        const val authRefreshTokenKey = "auth_refresh_token"
        const val friendsKey = "friends"
        const val latitudeKey = "lat"
        const val longitudeKey = "lng"
    }

}
