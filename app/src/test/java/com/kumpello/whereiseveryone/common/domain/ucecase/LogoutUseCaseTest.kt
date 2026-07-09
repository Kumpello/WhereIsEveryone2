package com.kumpello.whereiseveryone.common.domain.ucecase

import com.kumpello.whereiseveryone.common.database.AppDatabase
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LogoutUseCaseTest {

    private val encryptedDataStoreRepository: EncryptedDataStoreRepository = mockk()
    private val appDatabase: AppDatabase = mockk()
    private val logoutUseCase = LogoutUseCase(encryptedDataStoreRepository, appDatabase)

    @Test
    fun `execute clears data store and database tables`() = runTest {
        coEvery { encryptedDataStoreRepository.clearAll() } returns Unit
        coEvery { appDatabase.clearAllTables() } returns Unit

        logoutUseCase.execute()

        coVerify {
            encryptedDataStoreRepository.clearAll()
            appDatabase.clearAllTables()
        }
    }
}
