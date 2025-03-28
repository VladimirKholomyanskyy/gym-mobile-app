package com.neyra.gymapp.domain.usecase

import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.repository.TrainingProgramRepository
import com.neyra.gymapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for creating a new workout
 */
class CreateWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val trainingProgramRepository: TrainingProgramRepository
) {
    suspend operator fun invoke(
        trainingProgramId: String,
        name: String
    ): Result<Workout> {
        // First validate that the training program exists
        val programExists = trainingProgramRepository.trainingProgramExists(trainingProgramId)
        if (!programExists) {
            return Result.failure(IllegalArgumentException("Training program not found"))
        }

        // Create the workout domain object
        val workout = Workout(
            trainingProgramId = trainingProgramId,
            name = name
        )

        // Save the workout
        return workoutRepository.createWorkout(workout)
    }
}

/**
 * Use case for updating an existing workout
 */
class UpdateWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(
        workoutId: String,
        name: String? = null
    ): Result<Workout> {
        // Get the existing workout
        val existingWorkout = workoutRepository.getWorkout(workoutId)
            ?: return Result.failure(IllegalArgumentException("Workout not found"))

        // Only update fields that were provided
        val updatedWorkout = existingWorkout.copy(
            name = name ?: existingWorkout.name
        )
        if (name == null) {
            return Result.success(existingWorkout);
        }
        // Save the updated workout
        return workoutRepository.updateWorkout(workoutId, name)
    }
}

/**
 * Use case for deleting a workout
 */
class DeleteWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(workoutId: String): Result<Boolean> {
        return workoutRepository.deleteWorkout(workoutId)
    }
}

/**
 * Use case for reordering a workout
 */
class ReorderWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(workoutId: String, newPosition: Int): Result<Boolean> {
        return workoutRepository.reorderWorkout(workoutId, newPosition)
    }
}

/**
 * Use case for getting workouts by training program
 */
class GetWorkoutsUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(
        trainingProgramId: String,
        sortBy: WorkoutRepository.SortCriteria = WorkoutRepository.SortCriteria.POSITION
    ): Flow<List<Workout>> {
        return workoutRepository.getWorkouts(trainingProgramId, sortBy)
    }

    suspend fun getById(workoutId: String): Workout? {
        return workoutRepository.getWorkout(workoutId)
    }
}
