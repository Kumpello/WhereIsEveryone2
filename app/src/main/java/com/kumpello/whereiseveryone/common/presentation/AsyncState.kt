package com.kumpello.whereiseveryone.common.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable

@Stable
sealed class AsyncState<out T> {
    data object Idle : AsyncState<Nothing>()
    data class Loading(val message: String? = null, @StringRes val messageId: Int? = null) : AsyncState<Nothing>()
    data class Success<out T>(val data: T) : AsyncState<T>()
    data class Error(val throwable: Throwable? = null, @StringRes val messageId: Int? = null) : AsyncState<Nothing>()

    val isLoading: Boolean get() = this is Loading
}
