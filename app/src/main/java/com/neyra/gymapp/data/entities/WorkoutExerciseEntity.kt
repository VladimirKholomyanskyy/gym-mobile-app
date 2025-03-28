package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an exercise within a workout in the database
 */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workoutId"),
        Index("exerciseId"),
        Index(value = ["workoutId", "position"], unique = true)
    ]
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val position: Int,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val localCreatedAt: Long = System.currentTimeMillis(),
    val localUpdatedAt: Long = System.currentTimeMillis(),
    val serverCreatedAt: Long? = null,
    val serverUpdatedAt: Long? = null
)