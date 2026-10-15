package com.kumpello.whereiseveryone.data.repository.preferences

sealed class PreferencesKey<T>(val key: String) {
    data object AuthToken : PreferencesKey<String>("auth_token")
    data object AuthRefreshToken : PreferencesKey<String>("auth_refresh_token")
    data object UserName : PreferencesKey<String>("user_name")
    data object RememberedCredentials : PreferencesKey<String>("remembered_credentials")
    data object RestoreCredentialOwner : PreferencesKey<String>("restore_credential_owner")
    data object RestoreCredentialSyncedAt : PreferencesKey<String>("restore_credential_synced_at")
    data object PendingRestoreRegistration : PreferencesKey<String>("pending_restore_registration")
    data object RestoreSignOutPending : PreferencesKey<Boolean>("restore_sign_out_pending")
    data object UserMessage : PreferencesKey<String>("user_message")
    data object LocationSharingEnabled : PreferencesKey<Boolean>("location_sharing_enabled")
    data object CarMode : PreferencesKey<Boolean>("car_mode")
    data object MapFpsLimit : PreferencesKey<Int>("map_fps_limit")
    data object ProximityDistance : PreferencesKey<Int>("proximity_distance")
}
