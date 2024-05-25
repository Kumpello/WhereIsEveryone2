package com.kumpello.whereiseveryone.main.friends.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Friend(val nick : String)
