package com.kumpello.whereiseveryone.data.repository

import com.kumpello.whereiseveryone.data.local.database.UserLocationDao
import com.kumpello.whereiseveryone.data.local.database.UserLocationEntity
import com.kumpello.whereiseveryone.data.model.LocationData

class UserLocationRepository internal constructor(private val dao: UserLocationDao) {
    suspend fun getLastLocation(): LocationData? = dao.getUserLocation()?.let {
        LocationData(
            lat = it.latitude,
            lon = it.longitude,
            bearing = it.bearing,
            alt = it.altitude,
            accuracy = it.accuracy,
            speed = it.speed,
            lastUpdate = it.lastUpdate
        )
    }

    suspend fun saveLocation(location: LocationData) {
        dao.updateUserLocation(
            UserLocationEntity(
                latitude = location.lat,
                longitude = location.lon,
                bearing = location.bearing,
                altitude = location.alt,
                accuracy = location.accuracy,
                speed = location.speed,
                lastUpdate = location.lastUpdate
            )
        )
    }
}
