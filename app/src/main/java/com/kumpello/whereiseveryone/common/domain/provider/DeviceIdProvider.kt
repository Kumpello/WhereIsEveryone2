package com.kumpello.whereiseveryone.common.domain.provider

interface DeviceIdProvider {
    suspend fun getDeviceId(): String?
}
