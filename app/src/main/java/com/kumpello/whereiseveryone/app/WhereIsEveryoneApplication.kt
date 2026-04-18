package com.kumpello.whereiseveryone.app

import android.app.Application
import com.kumpello.whereiseveryone.BuildConfig
import com.kumpello.whereiseveryone.authentication.common.di.authenticationModule
import com.kumpello.whereiseveryone.main.common.di.mainModule
import com.kumpello.whereiseveryone.common.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber


class WhereIsEveryoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }

        startKoin{
            androidLogger()
            androidContext(this@WhereIsEveryoneApplication)
            modules(listOf(mainModule, authenticationModule, networkModule))
        }
    }

    companion object {

        const val PREFERENCES_NAME = "secret_keeper"
        const val USER_ID_KEY = "user_id"
        const val USER_NAME_KEY = "user_name"
        const val USER_MESSAGE_KEY = "user_message"
        const val AUTH_TOKEN_KEY = "auth_token"
        const val AUTH_REFRESH_TOKEN_KEY = "auth_refresh_token"
        const val FRIENDS_KEY = "friends"
        const val LATITUDE_KEY = "lat"
        const val LONGITUDE_KEY = "lng"
    }

}
