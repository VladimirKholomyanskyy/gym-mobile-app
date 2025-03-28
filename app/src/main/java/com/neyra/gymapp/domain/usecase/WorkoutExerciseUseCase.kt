package com.neyra.gymapp.domain.usecase

import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


/**
 * Use case for adding an exercise to a workout
 */
class AddExerciseToWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(
        workoutId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ): Result<WorkoutExercise> {
        // Create the workout exercise domain object
        val workoutExercise = WorkoutExercise.create(
            workoutId = workoutId,
            exerciseId = exerciseId,
            sets = sets,
            reps = reps
        )

        // Add the exercise to the workout
        return workoutRepository.addExerciseToWorkout(workoutExercise)
    }
}

/**
 * Use case for updating a workout exercise
 */
class UpdateWorkoutExerciseUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(
        workoutExerciseId: String,
        sets: Int? = null,
        reps: Int? = null
    ): Result<WorkoutExercise> {
        // Get the workout exercise details
        val exercises = workoutRepository.getWorkoutExercises(workoutExerciseId).map { list ->
            list.firstOrNull { it.id == workoutExerciseId }
        }

        val existingExercise = exercises.firstOrNull { it?.id == workoutExerciseId }
            ?: return Result.failure(IllegalArgumentException("Workout exercise not found"))

        // Update only provided fields
        val updatedExercise = existingExercise.copy(
            sets = sets ?: existingExercise.sets,
            reps = reps ?: existingExercise.reps
        )

        // Save updated exercise
        return workoutRepository.updateWorkoutExercise(workoutExerciseId, updatedExercise)
    }
}

/**
 * Use case for deleting a workout exercise
 */
class DeleteWorkoutExerciseUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(workoutExerciseId: String): Result<Boolean> {
        return workoutRepository.deleteWorkoutExercise(workoutExerciseId)
    }
}

/**
 * Use case for reordering a workout exercise
 */
class ReorderWorkoutExerciseUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(workoutExerciseId: String, newPosition: Int): Result<Boolean> {
        return workoutRepository.reorderWorkoutExercise(workoutExerciseId, newPosition)
    }
}

/**
 * Use case for getting workout exercises
 */
class GetWorkoutExercisesUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(workoutId: String): Flow<List<WorkoutExercise>> {
        return workoutRepository.getWorkoutExercises(workoutId)
    }
}