package com.kumpello.whereiseveryone.common.domain.manager

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesManager(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository
) {

    suspend fun <T> save(key: PreferencesKey<T>, value: T) {
        val prefKey = stringPreferencesKey(key.key)
        val stringValue = value.toString()
        val encryptedValue = encryptedDataStoreRepository.encrypt(stringValue)
        encryptedDataStoreRepository.dataStore().edit { it[prefKey] = encryptedValue }
    }

    suspend fun <T> get(key: PreferencesKey<T>): T? {
        val prefKey = stringPreferencesKey(key.key)
        val encryptedValue =
            encryptedDataStoreRepository.dataStore().data.map { it[prefKey] }.first()
        val decryptedValue = encryptedValue?.let { encryptedDataStoreRepository.decrypt(it) }

        return when (key) {
            is PreferencesKey.LocationSharingEnabled -> decryptedValue?.toBoolean() as T?
            else -> decryptedValue as T?
        }
    }

    suspend fun clearAll() {
        encryptedDataStoreRepository.clearAll()
    }
}
