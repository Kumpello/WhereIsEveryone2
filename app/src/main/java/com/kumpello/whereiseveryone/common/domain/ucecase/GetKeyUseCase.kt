package com.kumpello.whereiseveryone.common.domain.ucecase

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class GetKeyUseCase(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository,
) {

    fun getValue(key: String): String? {
        return runBlocking {
            val prefKey = stringPreferencesKey(key)
            val encryptedValue =
                encryptedDataStoreRepository.dataStore().data.map { it[prefKey] }.first()
            encryptedValue?.let { encryptedDataStoreRepository.decrypt(it) }
        }
    }

}
