package com.kumpello.whereiseveryone.app

import android.app.Application
import com.kumpello.whereiseveryone.BuildConfig
import com.kumpello.whereiseveryone.authentication.common.di.authenticationModule
import com.kumpello.whereiseveryone.common.di.commonModule
import com.kumpello.whereiseveryone.main.common.di.mainModule
import com.kumpello.whereiseveryone.common.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber


class WhereIsEveryoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        if(BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }

        startKoin{
            androidLogger()
            androidContext(this@WhereIsEveryoneApplication)
            modules(listOf(commonModule, mainModule, authenticationModule, networkModule))
        }
    }

    companion object {
        lateinit var instance: WhereIsEveryoneApplication
            private set
    }

}
