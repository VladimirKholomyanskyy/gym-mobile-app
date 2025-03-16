package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

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
    @PrimaryKey val id: UUID,
    val name: String,
    val trainingProgramId: UUID,
    val position: Int
)