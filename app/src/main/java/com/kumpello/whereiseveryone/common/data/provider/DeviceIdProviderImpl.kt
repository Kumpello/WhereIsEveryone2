package com.kumpello.whereiseveryone.common.data.provider

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.kumpello.whereiseveryone.common.domain.provider.DeviceIdProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class DeviceIdProviderImpl(private val context: Context) : DeviceIdProvider {
    override suspend fun getDeviceId(): String? {
        return try {
            val info = AppSet.getClient(context).appSetIdInfo.await()
            info.id
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to get AppSet ID, falling back to ANDROID_ID")
            getAndroidId()
        }
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String? {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    companion object {
        private const val TAG = "DeviceIdProvider"
    }
}
