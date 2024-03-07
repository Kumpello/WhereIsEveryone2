package com.kumpello.whereiseveryone.app

import android.app.Application
import com.kumpello.whereiseveryone.common.di.appModule
import com.kumpello.whereiseveryone.common.domain.model.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class WhereIsEveryoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger()
            androidContext(this@WhereIsEveryoneApplication)
            modules(listOf(appModule, networkModule))
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
