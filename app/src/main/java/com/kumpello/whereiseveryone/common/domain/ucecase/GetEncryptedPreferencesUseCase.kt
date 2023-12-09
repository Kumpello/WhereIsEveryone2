package com.kumpello.whereiseveryone.common.domain.ucecase

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication

class GetEncryptedPreferencesUseCase constructor(
    private val appContext: Context,
) {

    private val mainKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        WhereIsEveryoneApplication.preferencesName,
        mainKey,
        appContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun execute(): SharedPreferences {
        return sharedPreferences
    }

}