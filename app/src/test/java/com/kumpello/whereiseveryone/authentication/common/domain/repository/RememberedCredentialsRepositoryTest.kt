package com.kumpello.whereiseveryone.authentication.common.domain.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import com.kumpello.whereiseveryone.common.domain.repository.EncryptedDataStoreRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class RememberedCredentialsRepositoryTest {
    private val dataStore = MemoryDataStore()
    private val encryptedStore = mockk<EncryptedDataStoreRepository> {
        every { dataStore() } returns dataStore
        // A deterministic fake cipher lets these integration tests check the storage boundary.
        coEvery { encrypt(any()) } answers {
            Base64.getEncoder().encodeToString(firstArg<String>().toByteArray())
        }
        coEvery { decrypt(any()) } answers {
            String(Base64.getDecoder().decode(firstArg<String>()))
        }
        coEvery { clearAll() } coAnswers { dataStore.updateData { emptyPreferences() } }
    }
    private val preferences = PreferencesManager(encryptedStore)
    private val repository = RememberedCredentialsRepository(preferences)

    @Test
    fun `credentials round trip through one encrypted value and a fresh manager`() = runTest {
        val password = "päss\"word\\with\n特殊 characters"
        repository.save("username", password)
        coVerify(exactly = 1) { encryptedStore.encrypt(any()) }
        assertEquals(1, dataStore.data.value.asMap().size)
        assertFalse(dataStore.data.value.asMap().values.single().toString().contains("username"))

        val restored = RememberedCredentialsRepository(PreferencesManager(encryptedStore)).observe().first()
        assertEquals("username", restored?.username)
        assertEquals(password, restored?.password)
    }

    @Test
    fun `removing credentials clears persisted value and cache without removing session`() = runTest {
        repository.save("username", "password")
        preferences.save(PreferencesKey.AuthToken, "token")
        repository.clear()

        assertNull(repository.load())
        assertNull(preferences.getCached(PreferencesKey.RememberedCredentials))
        assertEquals("token", preferences.get(PreferencesKey.AuthToken))
    }

    @Test
    fun `logout preserves encrypted credentials and clears all other values and cache`() = runTest {
        repository.save("username", "password")
        preferences.save(PreferencesKey.AuthToken, "token")
        preferences.save(PreferencesKey.UserName, "username")
        val key = stringPreferencesKey(PreferencesKey.RememberedCredentials.key)
        val ciphertext = dataStore.data.value[key]

        preferences.clearSession()

        assertEquals(mapOf(key to ciphertext), dataStore.data.value.asMap())
        assertNull(preferences.getCached(PreferencesKey.AuthToken))
        assertNull(preferences.getCached(PreferencesKey.UserName))
        assertEquals("password", repository.load()?.password)
    }

    @Test
    fun `clear all also removes remembered credentials`() = runTest {
        repository.save("username", "password")
        preferences.clearAll()
        assertNull(repository.load())
        assertNull(preferences.getCached(PreferencesKey.RememberedCredentials))
    }

    @Test
    fun `malformed record is removed`() = runTest {
        preferences.save(PreferencesKey.RememberedCredentials, "{invalid-json}")
        assertNull(repository.observe().first())
        assertTrue(dataStore.data.value.asMap().isEmpty())
    }

    private class MemoryDataStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return transform(data.value).also { data.value = it }
        }
    }
}
