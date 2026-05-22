package com.kumpello.whereiseveryone.common.domain.services

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlin.time.Instant

object InstantAdapter {
    @ToJson
    fun toJson(value: Instant): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(value: String): Instant {
        return Instant.parse(value)
    }
}