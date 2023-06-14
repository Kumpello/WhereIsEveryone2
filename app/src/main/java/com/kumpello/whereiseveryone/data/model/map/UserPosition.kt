package com.kumpello.whereiseveryone.data.model.map

//TODO: Change location to LatLng or Location type, and maybe change uuid and nick to Friend type?
data class UserPosition(val uuid: String, val nick: String, val location: Pair<Double, Double>)
