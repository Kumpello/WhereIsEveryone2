package com.kumpello.whereiseveryone.data.session

import com.kumpello.whereiseveryone.data.local.database.AppDatabase
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogoutUseCase internal constructor(
    private val preferencesManager: PreferencesManager,
    private val appDatabase: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun execute() = withContext(ioDispatcher) {
        preferencesManager.clearSession()
        appDatabase.clearAllTables()
    }
}
