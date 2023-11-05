package com.kumpello.whereiseveryone.map.data.model

import com.google.android.gms.maps.model.LatLng

//TODO: Change location to LatLng or Location type, and maybe change uuid and nick to Friend type?
data class UserPosition(val uuid: String, val nick: String, val location: LatLng)
