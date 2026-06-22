package com.kumpello.whereiseveryone.common.domain.ucecase

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository

class SaveKeyUseCase(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository
) {

    suspend fun saveValue(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        val encryptedValue = encryptedDataStoreRepository.encrypt(value)
        encryptedDataStoreRepository.dataStore().edit { it[prefKey] = encryptedValue }
    }

}
