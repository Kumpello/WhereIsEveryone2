package com.kumpello.whereiseveryone.app

import android.app.Application
import android.content.Intent
import com.kumpello.whereiseveryone.BuildConfig
import com.kumpello.whereiseveryone.feature.authentication.di.authenticationModule
import com.kumpello.whereiseveryone.data.di.commonModule
import com.kumpello.whereiseveryone.feature.main.di.mainModule
import com.kumpello.whereiseveryone.main.MainActivity
import com.kumpello.whereiseveryone.feature.main.navigation.MainActivityIntentFactory
import com.kumpello.whereiseveryone.data.di.networkModule
import com.kumpello.whereiseveryone.app.logging.ProductionTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import timber.log.Timber


class WhereIsEveryoneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ProductionTree())

        startKoin{
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@WhereIsEveryoneApplication)
            modules(
                commonModule,
                mainModule,
                authenticationModule,
                networkModule(BuildConfig.BASE_URL),
                module {
                    single<MainActivityIntentFactory> {
                        MainActivityIntentFactory { context -> Intent(context, MainActivity::class.java) }
                    }
                }
            )
        }
    }

    companion object {
        lateinit var instance: WhereIsEveryoneApplication
            private set
    }

}
