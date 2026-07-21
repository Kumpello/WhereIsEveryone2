package com.kumpello.whereiseveryone.common.domain.usecase

import com.kumpello.whereiseveryone.common.database.AppDatabase
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogoutUseCase(
    private val preferencesManager: PreferencesManager,
    private val appDatabase: AppDatabase
) {
    suspend fun execute() = withContext(Dispatchers.IO) {
        preferencesManager.clearAll()
        appDatabase.clearAllTables()
    }
}
