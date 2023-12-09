package com.kumpello.whereiseveryone.main.map.data.model

import android.location.Location

//TODO: Change location to LatLng or Location type, and maybe change uuid and nick to Friend type?
data class UserPosition(val uuid: String, val nick: String, val location: Location)
