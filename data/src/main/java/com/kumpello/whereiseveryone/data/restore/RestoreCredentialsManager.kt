package com.kumpello.whereiseveryone.data.restore

import com.kumpello.whereiseveryone.data.device.DeviceIdProvider
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit

/** Coordinates system-managed restore keys without changing password sign-in. */
class RestoreCredentialsManager internal constructor(
    private val client: RestoreCredentialClient,
    private val api: RestoreCredentialsApi,
    private val preferences: PreferencesManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val appId: String,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()

    /** MainActivity covers new sign-ins, sign-ups and already signed-in users. */
    suspend fun createIfNeeded() {
        bestEffort("enrollment") {
            mutex.withLock {
                if (!client.isAvailable()) return@withLock
                val owner = preferences.get(PreferencesKey.UserName) ?: return@withLock
                val token = preferences.get(PreferencesKey.AuthToken) ?: return@withLock
                if (token.isBlank()) return@withLock
                val syncedOwner = preferences.get(PreferencesKey.RestoreCredentialOwner)
                val syncedAt = preferences.get(PreferencesKey.RestoreCredentialSyncedAt)?.toLongOrNull() ?: 0
                if (syncedOwner == owner && syncedAt in 1..nowMillis() &&
                    nowMillis() - syncedAt < KEY_RENEWAL_MILLIS) return@withLock

                // A previous logout may have failed to clear the platform key.
                // Complete that clear before enrolling the newly signed-in account.
                if (preferences.get(PreferencesKey.RestoreSignOutPending) == true) {
                    client.clear()
                    preferences.remove(PreferencesKey.RestoreSignOutPending)
                }
                val device = deviceIdProvider.getDeviceId()?.takeIf(String::isNotBlank) ?: return@withLock
                var pending = loadPending()
                if (pending != null && (pending.owner != owner || pending.deviceToken != device ||
                            pending.createdAt > nowMillis() ||
                            nowMillis() - pending.createdAt >= CHALLENGE_LIFETIME_MILLIS)) {
                    preferences.remove(PreferencesKey.PendingRestoreRegistration)
                    pending = null
                }
                if (pending == null) {
                    val options = api.registrationOptions("Bearer $token", RestoreOptionsRequest(appId, device))
                    val response = client.create(options.request_json)
                    pending = PendingRestoreRegistration(owner, device, options.challenge_id, response, nowMillis())
                    preferences.save(PreferencesKey.PendingRestoreRegistration, Json.encodeToString(pending))
                }
                if (!isCurrentSession(owner)) {
                    client.clear()
                    return@withLock
                }
                try {
                    val currentToken = preferences.get(PreferencesKey.AuthToken) ?: return@withLock
                    api.register("Bearer $currentToken", pending.toRequest(appId))
                } catch (exception: HttpException) {
                    if (exception.code() in listOf(400, 403)) {
                        preferences.remove(PreferencesKey.PendingRestoreRegistration)
                    }
                    throw exception
                }
                if (isCurrentSession(owner)) {
                    preferences.save(PreferencesKey.RestoreCredentialOwner, owner)
                    preferences.save(PreferencesKey.RestoreCredentialSyncedAt, nowMillis().toString())
                    preferences.remove(PreferencesKey.PendingRestoreRegistration)
                } else {
                    client.clear()
                }
            }
        }
    }

    /** Called from the launcher splash when no local session exists. */
    suspend fun restoreSession(): Boolean = bestEffort("sign-in") {
        mutex.withLock {
            if (!client.isAvailable() || preferences.get(PreferencesKey.RestoreSignOutPending) == true ||
                !preferences.get(PreferencesKey.AuthToken).isNullOrBlank()) return@withLock false
            val device = deviceIdProvider.getDeviceId()?.takeIf(String::isNotBlank) ?: return@withLock false
            val options = api.authenticationOptions(RestoreOptionsRequest(appId, device))
            val assertion = client.get(options.request_json) ?: return@withLock false
            val session = api.authenticate(RestoreFinishRequest(appId, device, options.challenge_id, assertion))
            // A logout can clear preferences while this request is in flight.
            if (preferences.get(PreferencesKey.RestoreSignOutPending) == true) return@withLock false
            preferences.saveRestoredSession(session.token, session.refresh_token, session.username)
        }
    } ?: false

    /** Leave server copies valid for device transfer; clear this device explicitly. */
    suspend fun clear() {
        // Preserve this guard across session cleanup, including platform failures.
        preferences.save(PreferencesKey.RestoreSignOutPending, true)
        bestEffort("clear") {
            mutex.withLock {
                preferences.remove(PreferencesKey.RestoreCredentialOwner)
                preferences.remove(PreferencesKey.RestoreCredentialSyncedAt)
                preferences.remove(PreferencesKey.PendingRestoreRegistration)
                if (client.isAvailable()) client.clear()
            }
        }
    }

    private suspend fun isCurrentSession(owner: String): Boolean =
        preferences.get(PreferencesKey.UserName) == owner &&
            !preferences.get(PreferencesKey.AuthToken).isNullOrBlank() &&
            preferences.get(PreferencesKey.RestoreSignOutPending) != true

    private suspend fun loadPending(): PendingRestoreRegistration? {
        val encoded = preferences.get(PreferencesKey.PendingRestoreRegistration) ?: return null
        return try {
            Json.decodeFromString<PendingRestoreRegistration>(encoded)
        } catch (_: IllegalArgumentException) {
            // Serialized WebAuthn payloads must never appear in diagnostic logs.
            preferences.remove(PreferencesKey.PendingRestoreRegistration)
            null
        }
    }

    private suspend fun <T> bestEffort(operation: String, block: suspend () -> T): T? = try {
        withTimeoutOrNull(OPERATION_TIMEOUT_MILLIS) { block() }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Timber.tag(TAG).d("Restore credential %s unavailable (%s)", operation, exception.javaClass.simpleName)
        null
    }

    private companion object {
        const val TAG = "RestoreCredentials"
        const val OPERATION_TIMEOUT_MILLIS = 15_000L
        const val CHALLENGE_LIFETIME_MILLIS = 5 * 60 * 1000L
        val KEY_RENEWAL_MILLIS = TimeUnit.DAYS.toMillis(180)
    }
}

private fun PendingRestoreRegistration.toRequest(appId: String) =
    RestoreFinishRequest(appId, deviceToken, challengeId, credentialJson)

@Serializable
private data class PendingRestoreRegistration(
    val owner: String,
    val deviceToken: String,
    val challengeId: String,
    val credentialJson: String,
    val createdAt: Long
)
