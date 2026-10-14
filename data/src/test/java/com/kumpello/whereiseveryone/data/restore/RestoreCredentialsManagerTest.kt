package com.kumpello.whereiseveryone.data.restore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kumpello.whereiseveryone.data.device.DeviceIdProvider
import com.kumpello.whereiseveryone.data.local.preferences.EncryptedDataStoreRepository
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

class RestoreCredentialsManagerTest {
    private val dataStore = MemoryDataStore()
    private val encryptedStore = mockk<EncryptedDataStoreRepository> {
        every { dataStore() } returns dataStore
        coEvery { encrypt(any()) } answers { Base64.getEncoder().encodeToString(firstArg<String>().toByteArray()) }
        coEvery { decrypt(any()) } answers { String(Base64.getDecoder().decode(firstArg<String>())) }
    }
    private val preferences = PreferencesManager(encryptedStore)
    private val client = mockk<RestoreCredentialClient>(relaxed = true) {
        every { isAvailable() } returns true
        coEvery { create(any()) } returns "registration-json"
        coEvery { this@mockk.get(any()) } returns "assertion-json"
    }
    private val api = mockk<RestoreCredentialsApi> {
        coEvery { registrationOptions(any(), any()) } returns RestoreOptionsResponse("registration-challenge", "creation-options")
        coEvery { register(any(), any()) } returns Unit
        coEvery { authenticationOptions(any()) } returns RestoreOptionsResponse("login-challenge", "get-options")
        coEvery { authenticate(any()) } returns RestoredSession("id", "alice", "new-access", "new-refresh")
    }
    private val deviceIdProvider = mockk<DeviceIdProvider> {
        coEvery { getDeviceId() } returns "device"
    }
    private var now = TimeUnit.DAYS.toMillis(1000)
    private val manager = RestoreCredentialsManager(client, api, preferences, deviceIdProvider, "app.id") { now }

    @Test
    fun `enrollment saves acknowledged key once and renews it before expiry`() = runTest {
        signIn()
        manager.createIfNeeded()
        manager.createIfNeeded()
        coVerify(exactly = 1) { client.create("creation-options") }
        assertEquals("alice", preferences.get(PreferencesKey.RestoreCredentialOwner))
        assertNull(preferences.get(PreferencesKey.PendingRestoreRegistration))

        now += TimeUnit.DAYS.toMillis(180)
        manager.createIfNeeded()
        coVerify(exactly = 2) { client.create(any()) }
    }

    @Test
    fun `lost registration response retries the same encrypted pending result`() = runTest {
        signIn()
        coEvery { api.register(any(), any()) } throws IOException("offline") andThen Unit
        manager.createIfNeeded()
        assertNull(preferences.get(PreferencesKey.RestoreCredentialOwner))
        assertTrue(preferences.get(PreferencesKey.PendingRestoreRegistration)?.contains("registration-json") == true)
        assertFalse(dataStore.data.value.asMap().values.any { it.toString().contains("registration-json") })

        manager.createIfNeeded()
        coVerify(exactly = 1) { client.create(any()) }
        coVerify(exactly = 2) {
            api.register("Bearer access", RestoreFinishRequest("app.id", "device", "registration-challenge", "registration-json"))
        }
        assertEquals("alice", preferences.get(PreferencesKey.RestoreCredentialOwner))
    }

    @Test
    fun `expired unacknowledged enrollment gets a fresh challenge and key`() = runTest {
        signIn()
        coEvery { api.register(any(), any()) } throws IOException("offline") andThen Unit
        manager.createIfNeeded()
        now += TimeUnit.MINUTES.toMillis(5)
        manager.createIfNeeded()
        coVerify(exactly = 2) { api.registrationOptions(any(), any()) }
        coVerify(exactly = 2) { client.create(any()) }
    }

    @Test
    fun `restored assertion commits a new session and enrollment follows`() = runTest {
        assertTrue(manager.restoreSession())
        assertEquals("new-access", preferences.get(PreferencesKey.AuthToken))
        assertEquals("new-refresh", preferences.get(PreferencesKey.AuthRefreshToken))
        assertEquals("alice", preferences.get(PreferencesKey.UserName))
        coVerify {
            api.authenticate(RestoreFinishRequest("app.id", "device", "login-challenge", "assertion-json"))
        }
        manager.createIfNeeded()
        coVerify(exactly = 1) { client.create(any()) }
    }

    @Test
    fun `missing credential and unavailable provider leave password sign-in usable`() = runTest {
        coEvery { client.get(any()) } returns null
        assertFalse(manager.restoreSession())
        assertNull(preferences.get(PreferencesKey.AuthToken))
        every { client.isAvailable() } returns false
        assertFalse(manager.restoreSession())
        coVerify(exactly = 1) { api.authenticationOptions(any()) }
        coVerify(exactly = 0) { api.authenticate(any()) }
    }

    @Test
    fun `failed key clear cannot silently undo logout and a new login retries clear`() = runTest {
        signIn()
        manager.createIfNeeded()
        coEvery { client.clear() } throws IOException("provider unavailable") andThen Unit
        manager.clear()
        preferences.clearSession()
        assertTrue(preferences.get(PreferencesKey.RestoreSignOutPending) == true)
        assertFalse(manager.restoreSession())
        coVerify(exactly = 0) { client.get(any()) }

        signIn()
        manager.createIfNeeded()
        coVerify(exactly = 2) { client.clear() }
        assertNull(preferences.get(PreferencesKey.RestoreSignOutPending))
        assertEquals("alice", preferences.get(PreferencesKey.RestoreCredentialOwner))
    }

    @Test
    fun `logout during restored authentication cannot repopulate session`() = runTest {
        val release = CompletableDeferred<Unit>()
        coEvery { api.authenticate(any()) } coAnswers {
            release.await()
            RestoredSession("id", "alice", "new-access", "new-refresh")
        }
        val restoring = async(start = CoroutineStart.UNDISPATCHED) { manager.restoreSession() }
        preferences.save(PreferencesKey.RestoreSignOutPending, true)
        preferences.clearSession()
        val clearing = async(start = CoroutineStart.UNDISPATCHED) { manager.clear() }
        release.complete(Unit)

        assertFalse(restoring.await())
        clearing.await()
        assertNull(preferences.get(PreferencesKey.AuthToken))
        coVerify(exactly = 1) { client.clear() }
    }

    @Test
    fun `logout during key creation clears the new key and skips registration`() = runTest {
        signIn()
        val release = CompletableDeferred<Unit>()
        coEvery { client.create(any()) } coAnswers { release.await(); "registration-json" }
        val creating = async(start = CoroutineStart.UNDISPATCHED) { manager.createIfNeeded() }
        preferences.save(PreferencesKey.RestoreSignOutPending, true)
        preferences.clearSession()
        val clearing = async(start = CoroutineStart.UNDISPATCHED) { manager.clear() }
        release.complete(Unit)
        creating.await()
        clearing.await()

        assertNull(preferences.get(PreferencesKey.RestoreCredentialOwner))
        assertNull(preferences.get(PreferencesKey.PendingRestoreRegistration))
        coVerify(exactly = 0) { api.register(any(), any()) }
        coVerify(atLeast = 1) { client.clear() }
    }

    @Test
    fun `restore cancellation propagates without saving session`() = runTest {
        coEvery { client.get(any()) } throws CancellationException("cancelled")
        try {
            manager.restoreSession()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertNull(preferences.get(PreferencesKey.AuthToken))
        }
    }

    @Test
    fun `session transaction refuses restoration after logout or another sign-in`() = runTest {
        preferences.save(PreferencesKey.RestoreSignOutPending, true)
        assertFalse(preferences.saveRestoredSession("access", "refresh", "alice"))
        assertNull(preferences.get(PreferencesKey.AuthToken))
        preferences.remove(PreferencesKey.RestoreSignOutPending)
        signIn()
        assertFalse(preferences.saveRestoredSession("other-access", "other-refresh", "bob"))
        assertEquals("access", preferences.get(PreferencesKey.AuthToken))
    }

    @Test
    fun `late refresh and rejection cannot change a replacement session`() = runTest {
        signIn()
        assertTrue(preferences.replaceSessionTokens("refresh", "rotated-access", "rotated-refresh"))
        assertFalse(preferences.invalidateSession("refresh"))
        assertFalse(preferences.invalidateAccessSession("access"))
        assertEquals("rotated-access", preferences.get(PreferencesKey.AuthToken))
        assertNull(preferences.get(PreferencesKey.RestoreSignOutPending))

        assertTrue(preferences.invalidateSession("rotated-refresh"))
        assertFalse(preferences.replaceSessionTokens("rotated-refresh", "late-access", "late-refresh"))
        assertNull(preferences.get(PreferencesKey.AuthToken))
        assertTrue(preferences.get(PreferencesKey.RestoreSignOutPending) == true)
    }

    private suspend fun signIn() {
        preferences.save(PreferencesKey.AuthToken, "access")
        preferences.save(PreferencesKey.AuthRefreshToken, "refresh")
        preferences.save(PreferencesKey.UserName, "alice")
    }

    private class MemoryDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
            transform(data.value).also { data.value = it }
        }
    }
}
