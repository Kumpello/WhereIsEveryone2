package com.kumpello.whereiseveryone.domain.events

sealed class SendLocationEvent {
    data class GetLocations(val longitude: Double, val latitude: Double) : SendLocationEvent()

}
