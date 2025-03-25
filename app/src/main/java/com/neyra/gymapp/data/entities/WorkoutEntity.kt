package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(
        entity = TrainingProgramEntity::class,
        parentColumns = ["id"],
        childColumns = ["trainingProgramId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trainingProgramId: String,
    val position: Int,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastModified: Long = System.currentTimeMillis()
)