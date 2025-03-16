package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class WorkoutSessionEntity(
    @PrimaryKey() val id: UUID,
    val profileId: UUID,
    val workoutId: UUID,
    val startTime: Long,
    val completionTime: Long,
)
