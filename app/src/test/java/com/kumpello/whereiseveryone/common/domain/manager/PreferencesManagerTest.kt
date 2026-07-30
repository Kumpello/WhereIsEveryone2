package com.kumpello.whereiseveryone.common.domain.manager

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesManagerTest {

    private val encryptedDataStoreRepository: EncryptedDataStoreRepository = mockk()
    private val dataStore: DataStore<Preferences> = mockk()
    private val preferencesManager = PreferencesManager(encryptedDataStoreRepository)

    @Test
    fun `save encrypts and stores value`() = runTest {
        every { encryptedDataStoreRepository.dataStore() } returns dataStore
        every { encryptedDataStoreRepository.encrypt("value") } returns "encrypted"
        
        // Mocking DataStore.edit is tricky, but we can verify it was called
        coEvery { dataStore.updateData(any()) } returns mockk()

        preferencesManager.save(PreferencesKey.AuthToken, "value")

        coVerify { 
            encryptedDataStoreRepository.encrypt("value")
            dataStore.updateData(any())
        }
    }

    @Test
    fun `get retrieves and decrypts value`() = runTest {
        val prefs = mockk<Preferences>()
        every { encryptedDataStoreRepository.dataStore() } returns dataStore
        every { dataStore.data } returns flowOf(prefs)
        every { prefs[any<Preferences.Key<String>>()] } returns "encrypted"
        every { encryptedDataStoreRepository.decrypt("encrypted") } returns "decrypted"

        val result = preferencesManager.get(PreferencesKey.AuthToken)

        assertEquals("decrypted", result)
    }

    @Test
    fun `clearAll calls repository clearAll`() = runTest {
        coEvery { encryptedDataStoreRepository.clearAll() } returns Unit

        preferencesManager.clearAll()

        coVerify { encryptedDataStoreRepository.clearAll() }
    }
}
