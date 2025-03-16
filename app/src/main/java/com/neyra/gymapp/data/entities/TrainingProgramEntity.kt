package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "training_programs")
data class TrainingProgramEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val description: String,
    val profileId: UUID,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Long = System.currentTimeMillis()
)