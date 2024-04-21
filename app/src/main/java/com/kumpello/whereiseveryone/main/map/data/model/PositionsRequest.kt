package com.kumpello.whereiseveryone.main.map.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PositionsRequest(val users: List<String>, val uuids: List<String>)
