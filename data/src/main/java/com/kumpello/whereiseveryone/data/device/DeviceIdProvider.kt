package com.kumpello.whereiseveryone.data.device

interface DeviceIdProvider {
    suspend fun getDeviceId(): String?
}
