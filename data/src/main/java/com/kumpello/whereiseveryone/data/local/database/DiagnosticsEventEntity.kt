package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostics_events")
internal data class DiagnosticsEventEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val payload: String,
    val ownerId: String?,
)
