package com.kumpello.whereiseveryone.main.map.domain

import com.kumpello.whereiseveryone.main.friends.model.UserInfo
import com.mapbox.geojson.Point

fun UserInfo.toPoint(): Point {
    return Point.fromLngLat(lon, lat, alt)
}