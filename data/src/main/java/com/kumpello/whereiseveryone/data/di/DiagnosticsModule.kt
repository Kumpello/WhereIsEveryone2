package com.kumpello.whereiseveryone.data.di

import com.kumpello.whereiseveryone.data.device.BatteryTelemetryMonitor
import com.kumpello.whereiseveryone.data.diagnostics.CrashReportStore
import com.kumpello.whereiseveryone.data.diagnostics.DiagnosticsReporter
import com.kumpello.whereiseveryone.data.diagnostics.PreviousProcessExitReporter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

fun diagnosticsModule(appVersion: String, enabled: Boolean, crashStore: CrashReportStore) = module {
    single { crashStore }
    single { DiagnosticsReporter(get(), get(), get(), get(), appVersion, enabled, get()) }
    single { BatteryTelemetryMonitor(androidContext(), get()) }
    single { PreviousProcessExitReporter(androidContext(), get()) }
}
