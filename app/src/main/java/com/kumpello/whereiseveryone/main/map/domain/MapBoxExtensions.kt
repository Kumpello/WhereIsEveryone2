package com.kumpello.whereiseveryone.main.map.domain

import com.kumpello.whereiseveryone.main.friends.model.Location
import com.mapbox.geojson.Point

fun Location.toPoint(): Point {
    return Point.fromLngLat(lon, lat, alt)
}