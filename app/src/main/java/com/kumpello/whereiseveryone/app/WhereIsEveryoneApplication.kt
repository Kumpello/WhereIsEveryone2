package com.kumpello.whereiseveryone.app

import android.app.Application
import com.kumpello.whereiseveryone.BuildConfig
import com.kumpello.whereiseveryone.authentication.common.di.authenticationModule
import com.kumpello.whereiseveryone.common.di.commonModule
import com.kumpello.whereiseveryone.main.common.di.mainModule
import com.kumpello.whereiseveryone.common.di.networkModule
import com.kumpello.whereiseveryone.common.logging.ProductionTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import timber.log.Timber


class WhereIsEveryoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ProductionTree())

        startKoin{
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@WhereIsEveryoneApplication)
            modules(listOf(commonModule, mainModule, authenticationModule, networkModule))
        }
    }

    companion object {
        lateinit var instance: WhereIsEveryoneApplication
            private set
    }

}
