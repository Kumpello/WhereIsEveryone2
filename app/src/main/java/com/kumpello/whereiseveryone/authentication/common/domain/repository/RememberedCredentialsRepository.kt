package com.kumpello.whereiseveryone.authentication.common.domain.repository

import com.kumpello.whereiseveryone.authentication.common.domain.model.RememberedCredentials
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class RememberedCredentialsRepository(private val preferencesManager: PreferencesManager) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): RememberedCredentials? {
        return decode(preferencesManager.get(PreferencesKey.RememberedCredentials))
    }

    fun observe(): Flow<RememberedCredentials?> = preferencesManager
        .observe(PreferencesKey.RememberedCredentials)
        .distinctUntilChanged()
        .map { decode(it) }

    private suspend fun decode(value: String?): RememberedCredentials? {
        if (value == null) return null
        return try {
            json.decodeFromString<RememberedCredentials>(value)
        } catch (_: SerializationException) {
            // Decoder errors may contain the input, so never log them.
            clear()
            null
        }
    }

    suspend fun save(username: String, password: String) {
        // One encrypted entry keeps the username and password consistent on disk.
        preferencesManager.save(
            PreferencesKey.RememberedCredentials,
            json.encodeToString(RememberedCredentials(username, password))
        )
    }

    suspend fun clear() {
        preferencesManager.remove(PreferencesKey.RememberedCredentials)
    }
}
