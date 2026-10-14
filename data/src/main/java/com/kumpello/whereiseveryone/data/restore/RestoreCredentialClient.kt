package com.kumpello.whereiseveryone.data.restore

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

internal interface RestoreCredentialClient {
    fun isAvailable(): Boolean
    suspend fun create(requestJson: String): String
    suspend fun get(requestJson: String): String?
    suspend fun clear()
}

internal class AndroidRestoreCredentialClient(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) : RestoreCredentialClient {
    override fun isAvailable(): Boolean = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(context, MIN_PLAY_SERVICES_VERSION) == ConnectionResult.SUCCESS

    override suspend fun create(requestJson: String): String {
        val response = try {
            credentialManager.createCredential(
                context,
                CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = true)
            )
        } catch (_: E2eeUnavailableException) {
            credentialManager.createCredential(
                context,
                CreateRestoreCredentialRequest(requestJson, isCloudBackupEnabled = false)
            )
        }
        return (response as CreateRestoreCredentialResponse).responseJson
    }

    override suspend fun get(requestJson: String): String? {
        val response = credentialManager.getCredential(
            context,
            GetCredentialRequest(listOf(GetRestoreCredentialOption(requestJson)))
        )
        return (response.credential as? RestoreCredential)?.authenticationResponseJson
    }

    override suspend fun clear() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL))
    }

    private companion object {
        const val MIN_PLAY_SERVICES_VERSION = 24220000
    }
}
