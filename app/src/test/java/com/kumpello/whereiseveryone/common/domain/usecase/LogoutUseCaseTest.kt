package com.kumpello.whereiseveryone.common.domain.usecase

import com.kumpello.whereiseveryone.common.database.AppDatabase
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LogoutUseCaseTest {

    private val preferencesManager: PreferencesManager = mockk()
    private val appDatabase: AppDatabase = mockk()
    private val logoutUseCase = LogoutUseCase(preferencesManager, appDatabase)

    @Test
    fun `execute clears data store and database tables`() = runTest {
        coEvery { preferencesManager.clearAll() } returns Unit
        coEvery { appDatabase.clearAllTables() } returns Unit

        logoutUseCase.execute()

        coVerify {
            preferencesManager.clearAll()
            appDatabase.clearAllTables()
        }
    }
}
