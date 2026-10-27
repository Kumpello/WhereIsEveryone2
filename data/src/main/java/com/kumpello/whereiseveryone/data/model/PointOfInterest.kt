package com.kumpello.whereiseveryone.data.model

const val MAX_POINTS_OF_INTEREST = 5
const val MAX_POINT_NAME_LENGTH = 64
const val MAX_POINT_DESCRIPTION_LENGTH = 1_000

fun isValidPointText(name: String, description: String): Boolean =
    name.isNotBlank() && name.codePointCount(0, name.length) <= MAX_POINT_NAME_LENGTH &&
        description.codePointCount(0, description.length) <= MAX_POINT_DESCRIPTION_LENGTH

data class PointOfInterest(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val creatorUsername: String,
    val createdAt: Long,
    val isOwn: Boolean = false,
)
