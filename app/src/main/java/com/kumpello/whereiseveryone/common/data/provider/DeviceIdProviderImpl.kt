package com.kumpello.whereiseveryone.common.data.provider

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.kumpello.whereiseveryone.common.domain.provider.DeviceIdProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class DeviceIdProviderImpl(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : DeviceIdProvider {
    override suspend fun getDeviceId(): String? {
        return try {
            val info = AppSet.getClient(context).appSetIdInfo.await()
            info.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to get AppSet ID, falling back to ANDROID_ID")
            withContext(ioDispatcher) { getAndroidId() }
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
