package com.kumpello.whereiseveryone.common.domain.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class EncryptedDataStoreRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    aeadFactory: () -> Aead = { createAead(context) }
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)

    // Keyset initialization touches SharedPreferences and Android Keystore.
    // Both initialization and crypto operations are confined to ioDispatcher.
    private val aead: Aead by lazy(aeadFactory)

    fun dataStore(): DataStore<Preferences> = context.dataStore

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun encrypt(value: String): String = withContext(ioDispatcher) {
        val encrypted = aead.encrypt(value.toByteArray(), null)
        Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    suspend fun decrypt(encryptedValue: String): String? = withContext(ioDispatcher) {
        try {
            val decoded = Base64.decode(encryptedValue, Base64.NO_WRAP)
            String(aead.decrypt(decoded, null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Decryption failed")
            null
        }
    }

    companion object {
        private fun createAead(context: Context): Aead {
            AeadConfig.register()
            return AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        }

        private const val PREFS_NAME = "secure_datastore"
        private const val KEYSET_NAME = "master_keyset"
        private const val PREF_FILE_NAME = "master_key_preference"
        private const val MASTER_KEY_URI = "android-keystore://master_key"
        private const val TAG = "DATA_STORE_REPO"
    }
}
