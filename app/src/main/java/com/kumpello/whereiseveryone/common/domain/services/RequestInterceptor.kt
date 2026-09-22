package com.kumpello.whereiseveryone.common.domain.services

import okhttp3.Interceptor
import okhttp3.Response

import timber.log.Timber

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Timber.tag("HTTP").d("Outgoing %s request", request.method)
        return chain.proceed(request)
    }
}