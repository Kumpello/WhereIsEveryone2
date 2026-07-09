package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.common.database.AppDatabase
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val encryptedDataStoreRepository: EncryptedDataStoreRepository,
    private val appDatabase: AppDatabase
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        encryptedDataStoreRepository.clearAll()
        appDatabase.clearAllTables()
    }
}
