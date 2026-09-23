package com.kumpello.whereiseveryone.common.logging

import android.util.Log
import timber.log.Timber

/** Log status codes only: response bodies and reason phrases can contain personal data. */
internal fun Timber.Tree.httpFailure(operation: String, statusCode: Int) {
    log(
        if (statusCode >= 500) Log.ERROR else Log.WARN,
        "%s failed (HTTP %d)",
        operation,
        statusCode
    )
}
