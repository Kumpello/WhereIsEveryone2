package com.kumpello.whereiseveryone.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface DiagnosticsEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: DiagnosticsEventEntity)

    @Query(
        "SELECT * FROM diagnostics_events WHERE ownerId = :ownerId " +
            "ORDER BY createdAt ASC, id ASC LIMIT :limit"
    )
    suspend fun oldest(ownerId: String, limit: Int): List<DiagnosticsEventEntity>

    @Query("DELETE FROM diagnostics_events WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query(
        "DELETE FROM diagnostics_events WHERE id NOT IN " +
            "(SELECT id FROM diagnostics_events ORDER BY createdAt DESC, id DESC LIMIT :maxCount)"
    )
    suspend fun prune(maxCount: Int)

    @Query("DELETE FROM diagnostics_events WHERE createdAt < :cutoff")
    suspend fun purgeBefore(cutoff: Long)
}
