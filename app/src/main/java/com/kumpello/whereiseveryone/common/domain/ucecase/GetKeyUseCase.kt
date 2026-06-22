package com.kumpello.whereiseveryone.common.domain.ucecase

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GetKeyUseCase(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository,
) {

    suspend fun getValue(key: String): String? {
        val prefKey = stringPreferencesKey(key)
        val encryptedValue =
            encryptedDataStoreRepository.dataStore().data.map { it[prefKey] }.first()
        return encryptedValue?.let { encryptedDataStoreRepository.decrypt(it) }
    }

}
