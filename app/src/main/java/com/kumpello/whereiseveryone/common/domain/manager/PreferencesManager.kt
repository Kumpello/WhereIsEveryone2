package com.kumpello.whereiseveryone.common.domain.manager

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class PreferencesManager(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository
) {
    private val cache = ConcurrentHashMap<String, Any>()

    suspend fun <T> save(key: PreferencesKey<T>, value: T) {
        val prefKey = stringPreferencesKey(key.key)
        val stringValue = value.toString()
        if (value != null) {
            cache[key.key] = value
        } else {
            cache.remove(key.key)
        }
        val encryptedValue = encryptedDataStoreRepository.encrypt(stringValue)
        encryptedDataStoreRepository.dataStore().edit { it[prefKey] = encryptedValue }
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

    @Suppress("UNCHECKED_CAST")
    private fun <T> mapValue(key: PreferencesKey<T>, value: String?): T? {
        return when (key) {
            is PreferencesKey.LocationSharingEnabled -> value?.toBoolean() as T?
            is PreferencesKey.ProximityDistance -> value?.toIntOrNull() as T?
            else -> value as T?
        }
    }

    suspend fun clearAll() {
        cache.clear()
        encryptedDataStoreRepository.clearAll()
    }
}
