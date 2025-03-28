package com.neyra.gymapp.domain.mapper

import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.WorkoutEntity
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import java.util.UUID

/**
 * Converts a WorkoutEntity to a domain Workout
 * @param exercises The workout exercises associated with this workout
 * @return Mapped Workout domain model
 */
fun WorkoutEntity.toDomain(exercises: List<WorkoutExercise> = emptyList()): Workout {
    return Workout(
        id = this.id,
        trainingProgramId = this.trainingProgramId,
        name = this.name,
        position = this.position,
        exercises = exercises,

        )
}

/**
 * Converts a domain Workout to a WorkoutEntity
 * @return Mapped WorkoutEntity for data layer
 */
fun Workout.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING_CREATE): WorkoutEntity {
    val now = System.currentTimeMillis()
    return WorkoutEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        trainingProgramId = this.trainingProgramId,
        name = this.name,
        position = this.position,
        localCreatedAt = now,
        localUpdatedAt = now,
        serverCreatedAt = null,
        serverUpdatedAt = null,
        syncStatus = syncStatus
    )
}




