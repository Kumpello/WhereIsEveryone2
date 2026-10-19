package com.kumpello.whereiseveryone.feature.main.navigation

import android.content.Context
import android.content.Intent

fun interface MainActivityIntentFactory {
    fun create(context: Context): Intent

    companion object {
        const val ACTION_OPEN_FRIEND_REQUESTS = "com.kumpello.whereiseveryone.OPEN_FRIEND_REQUESTS"
    }
}
