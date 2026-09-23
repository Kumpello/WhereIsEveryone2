package com.kumpello.whereiseveryone.common.domain.usecase

import com.kumpello.whereiseveryone.common.database.AppDatabase
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val preferencesManager: PreferencesManager,
    private val appDatabase: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun execute() = withContext(ioDispatcher) {
        preferencesManager.clearAll()
        appDatabase.clearAllTables()
    }
}
