package com.neyra.gymapp.domain.mapper

import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.WorkoutEntity
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
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
        exercises = exercises
    )
}

/**
 * Converts a domain Workout to a WorkoutEntity
 * @return Mapped WorkoutEntity for data layer
 */
fun Workout.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING_CREATE): WorkoutEntity {
    return WorkoutEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        trainingProgramId = this.trainingProgramId,
        name = this.name,
        position = this.position
    )
}

/**
 * Converts a WorkoutExerciseEntity to a domain WorkoutExercise
 * @param exerciseName Optional name of the exercise (from the Exercise entity)
 * @param primaryMuscle Optional primary muscle of the exercise (from the Exercise entity)
 * @return Mapped WorkoutExercise domain model
 */
fun WorkoutExerciseEntity.toDomain(
    exerciseName: String = "",
    primaryMuscle: String = ""
): WorkoutExercise {
    return WorkoutExercise(
        id = this.id,
        workoutId = this.workoutId,
        exerciseId = this.exerciseId,
        sets = this.sets,
        reps = this.reps,
        position = this.position,
        exerciseName = exerciseName,
        primaryMuscle = primaryMuscle
    )
}

/**
 * Converts a domain WorkoutExercise to a WorkoutExerciseEntity
 * @return Mapped WorkoutExerciseEntity for data layer
 */
fun WorkoutExercise.toEntity(): WorkoutExerciseEntity {
    return WorkoutExerciseEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        workoutId = this.workoutId,
        exerciseId = this.exerciseId,
        sets = this.sets,
        reps = this.reps,
        position = this.position
    )
}

/**
 * Converts a list of WorkoutEntities to domain Workouts
 * @param exercisesMap Map of workout ID to list of exercises for that workout
 * @return List of mapped Workout domain models
 */
fun List<WorkoutEntity>.toDomainList(
    exercisesMap: Map<String, List<WorkoutExercise>> = emptyMap()
): List<Workout> {
    return map { entity ->
        entity.toDomain(exercisesMap[entity.id] ?: emptyList())
    }
}

/**
 * Converts a list of domain Workouts to WorkoutEntities
 * @return List of mapped WorkoutEntities
 */
fun List<Workout>.toEntityList(): List<WorkoutEntity> {
    return map { it.toEntity() }
}

/**
 * Converts a list of WorkoutExerciseEntities to domain WorkoutExercises
 * @param exerciseInfoMap Map of exercise ID to Pair(name, primaryMuscle)
 * @return List of mapped WorkoutExercise domain models
 */
fun List<WorkoutExerciseEntity>.toDomainExerciseList(
    exerciseInfoMap: Map<String, Pair<String, String>> = emptyMap()
): List<WorkoutExercise> {
    return map { entity ->
        val (name, muscle) = exerciseInfoMap[entity.exerciseId] ?: ("" to "")
        entity.toDomain(name, muscle)
    }
}

/**
 * Converts a list of domain WorkoutExercises to WorkoutExerciseEntities
 * @return List of mapped WorkoutExerciseEntities
 */
fun List<WorkoutExercise>.toEntityExerciseList(): List<WorkoutExerciseEntity> {
    return map { it.toEntity() }
}