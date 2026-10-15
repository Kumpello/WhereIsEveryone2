package com.kumpello.whereiseveryone.data.repository.preferences

import androidx.annotation.WorkerThread
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.data.local.preferences.EncryptedDataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class PreferencesManager internal constructor(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository
) {
    private val cache = ConcurrentHashMap<String, Any>()

    suspend fun <T> save(key: PreferencesKey<T>, value: T) {
        val prefKey = stringPreferencesKey(key.key)
        val stringValue = value.toString()
        val encryptedValue = encryptedDataStoreRepository.encrypt(stringValue)
        encryptedDataStoreRepository.dataStore().edit { it[prefKey] = encryptedValue }
        if (value != null) {
            cache[key.key] = value
        } else {
            cache.remove(key.key)
        }
    }

    suspend fun <T> get(key: PreferencesKey<T>): T? {
        val prefKey = stringPreferencesKey(key.key)
        val encryptedValue =
            encryptedDataStoreRepository.dataStore().data.map { it[prefKey] }.first()
        val decryptedValue = encryptedValue?.let { encryptedDataStoreRepository.decrypt(it) }

        val mapped = mapValue(key, decryptedValue)
        if (mapped != null) {
            cache[key.key] = mapped
        } else {
            cache.remove(key.key)
        }
        return mapped
    }

    /** Blocking cache-miss bridge for synchronous OkHttp interceptors only. */
    @WorkerThread
    @Suppress("UNCHECKED_CAST")
    fun <T> getCached(key: PreferencesKey<T>): T? {
        val cachedValue = cache[key.key] as? T
        if (cachedValue != null) return cachedValue
        return runBlocking { get(key) }
    }

    fun <T> observe(key: PreferencesKey<T>): Flow<T?> {
        val prefKey = stringPreferencesKey(key.key)
        return encryptedDataStoreRepository.dataStore().data.map { prefs ->
            val encryptedValue = prefs[prefKey]
            val decryptedValue = encryptedValue?.let { encryptedDataStoreRepository.decrypt(it) }
            val mapped = mapValue(key, decryptedValue)
            if (mapped != null) {
                cache[key.key] = mapped
            } else {
                cache.remove(key.key)
            }
            mapped
        }
    }

    suspend fun clearAll() {
        cache.clear()
        encryptedDataStoreRepository.clearAll()
    }

    suspend fun remove(key: PreferencesKey<*>) {
        encryptedDataStoreRepository.dataStore().edit { it.remove(stringPreferencesKey(key.key)) }
        cache.remove(key.key)
    }

    /** Logout removes session data while retaining explicitly remembered credentials. */
    suspend fun clearSession() {
        val rememberedKey = stringPreferencesKey(PreferencesKey.RememberedCredentials.key)
        val restoreGuardKey = stringPreferencesKey(PreferencesKey.RestoreSignOutPending.key)
        encryptedDataStoreRepository.dataStore().edit { preferences ->
            val remembered = preferences[rememberedKey]
            val restoreGuard = preferences[restoreGuardKey]
            preferences.clear()
            if (remembered != null) preferences[rememberedKey] = remembered
            if (restoreGuard != null) preferences[restoreGuardKey] = restoreGuard
        }
        cache.clear()
    }

    /** Atomically commits restored tokens only if a logout has not intervened. */
    suspend fun saveRestoredSession(token: String, refreshToken: String, username: String): Boolean {
        val authKey = stringPreferencesKey(PreferencesKey.AuthToken.key)
        val refreshKey = stringPreferencesKey(PreferencesKey.AuthRefreshToken.key)
        val usernameKey = stringPreferencesKey(PreferencesKey.UserName.key)
        val restoreGuardKey = stringPreferencesKey(PreferencesKey.RestoreSignOutPending.key)
        val encryptedToken = encryptedDataStoreRepository.encrypt(token)
        val encryptedRefresh = encryptedDataStoreRepository.encrypt(refreshToken)
        val encryptedUsername = encryptedDataStoreRepository.encrypt(username)
        var saved = false
        encryptedDataStoreRepository.dataStore().edit { preferences ->
            val blocked = preferences[restoreGuardKey]?.let { encryptedDataStoreRepository.decrypt(it) }?.toBoolean() == true
            if (!blocked && preferences[authKey] == null) {
                preferences[authKey] = encryptedToken
                preferences[refreshKey] = encryptedRefresh
                preferences[usernameKey] = encryptedUsername
                saved = true
            }
        }
        // Avoid repopulating the cache after a concurrent logout. Subsequent
        // reads populate it from the authoritative DataStore transaction.
        cache.remove(PreferencesKey.AuthToken.key)
        cache.remove(PreferencesKey.AuthRefreshToken.key)
        cache.remove(PreferencesKey.UserName.key)
        return saved
    }

    /** A refresh response cannot resurrect credentials removed by logout. */
    suspend fun replaceSessionTokens(expectedRefresh: String, token: String, refreshToken: String): Boolean {
        val authKey = stringPreferencesKey(PreferencesKey.AuthToken.key)
        val refreshKey = stringPreferencesKey(PreferencesKey.AuthRefreshToken.key)
        val encryptedToken = encryptedDataStoreRepository.encrypt(token)
        val encryptedRefresh = encryptedDataStoreRepository.encrypt(refreshToken)
        var saved = false
        encryptedDataStoreRepository.dataStore().edit { preferences ->
            val currentRefresh = preferences[refreshKey]?.let { encryptedDataStoreRepository.decrypt(it) }
            if (currentRefresh == expectedRefresh && preferences[authKey] != null) {
                preferences[authKey] = encryptedToken
                preferences[refreshKey] = encryptedRefresh
                saved = true
            }
        }
        cache.remove(PreferencesKey.AuthToken.key)
        cache.remove(PreferencesKey.AuthRefreshToken.key)
        return saved
    }

    /** Ignore a late rejection of an older session after another sign-in. */
    suspend fun invalidateSession(expectedRefresh: String): Boolean =
        invalidateSession(PreferencesKey.AuthRefreshToken, expectedRefresh)

    suspend fun invalidateAccessSession(expectedToken: String): Boolean =
        invalidateSession(PreferencesKey.AuthToken, expectedToken)

    private suspend fun invalidateSession(key: PreferencesKey<String>, expectedValue: String): Boolean {
        val sessionKey = stringPreferencesKey(key.key)
        val rememberedKey = stringPreferencesKey(PreferencesKey.RememberedCredentials.key)
        val restoreGuardKey = stringPreferencesKey(PreferencesKey.RestoreSignOutPending.key)
        val encryptedGuard = encryptedDataStoreRepository.encrypt(true.toString())
        var invalidated = false
        encryptedDataStoreRepository.dataStore().edit { preferences ->
            val currentValue = preferences[sessionKey]?.let { encryptedDataStoreRepository.decrypt(it) }
            if (currentValue == expectedValue) {
                val remembered = preferences[rememberedKey]
                preferences.clear()
                if (remembered != null) preferences[rememberedKey] = remembered
                preferences[restoreGuardKey] = encryptedGuard
                invalidated = true
            }
        }
        cache.clear()
        return invalidated
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> mapValue(key: PreferencesKey<T>, value: String?): T? {
        return when (key) {
            is PreferencesKey.LocationSharingEnabled, is PreferencesKey.CarMode,
            is PreferencesKey.RestoreSignOutPending -> value?.toBoolean() as T?
            is PreferencesKey.ProximityDistance, is PreferencesKey.MapFpsLimit -> value?.toIntOrNull() as T?
            else -> value as T?
        }
    }
}
