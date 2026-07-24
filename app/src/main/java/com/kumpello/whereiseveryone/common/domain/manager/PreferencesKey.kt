package com.kumpello.whereiseveryone.common.domain.manager

sealed class PreferencesKey<T>(val key: String) {
    data object AuthToken : PreferencesKey<String>("auth_token")
    data object AuthRefreshToken : PreferencesKey<String>("auth_refresh_token")
    data object UserName : PreferencesKey<String>("user_name")
    data object UserMessage : PreferencesKey<String>("user_message")
    data object LocationSharingEnabled : PreferencesKey<Boolean>("location_sharing_enabled")
    data object ProximityDistance : PreferencesKey<Int>("proximity_distance")
}
