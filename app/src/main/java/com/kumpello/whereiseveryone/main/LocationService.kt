package com.kumpello.whereiseveryone.main

interface LocationService {
    fun changeForegroundUpdateInterval(interval: Int)
    fun changeBackgroundUpdateInterval(interval: Int)
    fun setUpdateInterval(type: UpdateInterval)

    sealed interface UpdateInterval {
        data object Foreground : UpdateInterval
        data object Background : UpdateInterval
    }
}