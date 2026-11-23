package com.kumpello.whereiseveryone.app

import android.app.Application
import android.content.Intent
import com.kumpello.whereiseveryone.BuildConfig
import com.kumpello.whereiseveryone.feature.authentication.di.authenticationModule
import com.kumpello.whereiseveryone.data.device.BatteryTelemetryMonitor
import com.kumpello.whereiseveryone.data.di.commonModule
import com.kumpello.whereiseveryone.data.di.diagnosticsModule
import com.kumpello.whereiseveryone.data.diagnostics.CrashReportStore
import com.kumpello.whereiseveryone.data.diagnostics.DiagnosticsReporter
import com.kumpello.whereiseveryone.data.diagnostics.PreviousProcessExitReporter
import com.kumpello.whereiseveryone.feature.main.di.mainModule
import com.kumpello.whereiseveryone.main.MainActivity
import com.kumpello.whereiseveryone.feature.main.navigation.MainActivityIntentFactory
import com.kumpello.whereiseveryone.data.di.networkModule
import com.kumpello.whereiseveryone.app.logging.ProductionCrashHandler
import com.kumpello.whereiseveryone.app.logging.ProductionTree
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import timber.log.Timber
import java.io.File


class WhereIsEveryoneApplication : Application() {
    @Volatile private var diagnosticsReporter: DiagnosticsReporter? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        val crashStore = CrashReportStore(File(filesDir, "diagnostics-crash.json"), BuildConfig.VERSION_NAME)
        if (!BuildConfig.DEBUG) {
            Thread.getDefaultUncaughtExceptionHandler()?.let { previous ->
                Thread.setDefaultUncaughtExceptionHandler(ProductionCrashHandler(previous) { error ->
                    diagnosticsReporter?.saveFatal(error) ?: crashStore.save(error)
                })
            }
        }
        val productionTree = if (BuildConfig.DEBUG) null else ProductionTree()
        Timber.plant(productionTree ?: Timber.DebugTree())

        val koinApplication = startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@WhereIsEveryoneApplication)
            modules(
                commonModule,
                diagnosticsModule(BuildConfig.VERSION_NAME, enabled = !BuildConfig.DEBUG, crashStore = crashStore),
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
        if (!BuildConfig.DEBUG) {
            try {
                val reporter = koinApplication.koin.get<DiagnosticsReporter>()
                diagnosticsReporter = reporter
                productionTree?.attachReporter { component, message, throwable ->
                    reporter.reportError(component, message, throwable)
                }
                reporter.start()
                koinApplication.koin.get<PreviousProcessExitReporter>().start()
                koinApplication.koin.get<BatteryTelemetryMonitor>().start()
            } catch (exception: Exception) {
                Timber.tag("Diagnostics").e(exception, "Unable to start diagnostics")
            }
        }
    }

    companion object {
        lateinit var instance: WhereIsEveryoneApplication
            private set
    }

}
