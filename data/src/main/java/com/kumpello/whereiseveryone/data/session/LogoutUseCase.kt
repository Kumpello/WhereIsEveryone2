package com.kumpello.whereiseveryone.data.session

import com.kumpello.whereiseveryone.data.local.database.AppDatabase
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.restore.RestoreCredentialsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class LogoutUseCase internal constructor(
    private val preferencesManager: PreferencesManager,
    private val appDatabase: AppDatabase,
    private val restoreCredentialsManager: RestoreCredentialsManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun execute(
        expectedRefreshToken: String? = null,
        expectedAccessToken: String? = null
    ) = withContext(ioDispatcher + NonCancellable) {
        // MainActivity closes as soon as the token disappears. Complete this
        // bounded credential cleanup even when its ViewModel is then destroyed.
        if (expectedRefreshToken != null) {
            if (!preferencesManager.invalidateSession(expectedRefreshToken)) return@withContext
        } else if (expectedAccessToken != null) {
            if (!preferencesManager.invalidateAccessSession(expectedAccessToken)) return@withContext
        } else {
            preferencesManager.save(PreferencesKey.RestoreSignOutPending, true)
            preferencesManager.clearSession()
        }
        try {
            appDatabase.clearAllTables()
        } finally {
            restoreCredentialsManager.clear()
        }
    }
}
