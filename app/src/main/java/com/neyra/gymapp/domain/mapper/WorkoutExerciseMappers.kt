package com.neyra.gymapp.domain.mapper

import com.neyra.gymapp.data.entities.ExerciseEntity
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
import com.neyra.gymapp.domain.model.Exercise
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.model.toDomain
import java.util.UUID

/**
 * Converts a WorkoutExerciseEntity to a domain WorkoutExercise
 * @param exercise The associated Exercise domain model
 * @return Mapped WorkoutExercise domain model
 */
fun WorkoutExerciseEntity.toDomain(exercise: Exercise): WorkoutExercise {
    return WorkoutExercise(
        id = this.id,
        workoutId = this.workoutId,
        exercise = exercise,
        sets = this.sets,
        reps = this.reps,
        position = this.position
    )
}

/**
 * Converts a domain WorkoutExercise to a WorkoutExerciseEntity
 * @param syncStatus Optional sync status for the entity
 * @return Mapped WorkoutExerciseEntity for data layer
 */
fun WorkoutExercise.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING_CREATE): WorkoutExerciseEntity {
    return WorkoutExerciseEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        workoutId = this.workoutId,
        exerciseId = this.exercise.id
            ?: throw IllegalArgumentException("Exercise ID cannot be null"),
        sets = this.sets,
        reps = this.reps,
        position = this.position
    )
}

/**
 * Converts a list of WorkoutExerciseEntity to domain WorkoutExercises
 * @param exerciseInfoMap Map of exercise IDs to pairs of exercise name and primary muscle
 * @return List of mapped WorkoutExercise domain models
 */
fun List<WorkoutExerciseEntity>.toDomainExerciseList(
    exerciseInfoMap: Map<String, Pair<String, String>>
): List<WorkoutExercise> {
    return this.mapNotNull { entity ->
        val (name, primaryMuscle) = exerciseInfoMap[entity.exerciseId] ?: return@mapNotNull null

        // Create minimal Exercise model with available info
        val exercise = Exercise(
            id = entity.exerciseId,
            name = name,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = emptyList(),
            equipment = "",
            description = ""
        )

        entity.toDomain(exercise)
    }
}

/**
 * Converts a WorkoutExerciseEntity and ExerciseEntity to a domain WorkoutExercise
 * @param exerciseEntity The associated ExerciseEntity
 * @return Mapped WorkoutExercise domain model
 */
fun WorkoutExerciseEntity.toDomain(exerciseEntity: ExerciseEntity): WorkoutExercise {
    return toDomain(exerciseEntity.toDomain())
}