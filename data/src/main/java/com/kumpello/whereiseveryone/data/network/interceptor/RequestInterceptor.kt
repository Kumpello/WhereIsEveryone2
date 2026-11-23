package com.kumpello.whereiseveryone.data.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation
import timber.log.Timber
import java.io.IOException

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Retrofit method names are code identifiers, never paths, tokens, or user data.
        val method = request.tag(Invocation::class.java)?.method()
        val operation = method?.let { "${it.declaringClass.simpleName}.${it.name}" } ?: "HTTP request"
        Timber.tag("HTTP").d("Outgoing %s request", request.method)
        return try {
            chain.proceed(request).also { response ->
                if (!response.isSuccessful) {
                    Timber.tag("HTTP").log(
                        if (response.code >= 500) Log.ERROR else Log.WARN,
                        "%s failed (HTTP %d)", operation, response.code,
                    )
                }
            }
        } catch (exception: IOException) {
            if (!chain.call().isCanceled()) {
                Timber.tag("HTTP").w(exception, "%s transport failed", operation)
            }
            throw exception
        }
    }
}