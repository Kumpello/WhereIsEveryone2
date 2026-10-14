package com.kumpello.whereiseveryone.data.session

import com.kumpello.whereiseveryone.data.local.database.AppDatabase
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.restore.RestoreCredentialsManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LogoutUseCaseTest {

    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk()
    private val restoreCredentialsManager: RestoreCredentialsManager = mockk(relaxed = true)
    private val logoutUseCase = LogoutUseCase(preferencesManager, appDatabase, restoreCredentialsManager)

    @Test
    fun `execute clears session data and database tables`() = runTest {
        coEvery { preferencesManager.clearSession() } returns Unit
        coEvery { appDatabase.clearAllTables() } returns Unit

        logoutUseCase.execute()

        coVerify {
            preferencesManager.save(PreferencesKey.RestoreSignOutPending, true)
            preferencesManager.clearSession()
            appDatabase.clearAllTables()
            restoreCredentialsManager.clear()
        }
    }
}
