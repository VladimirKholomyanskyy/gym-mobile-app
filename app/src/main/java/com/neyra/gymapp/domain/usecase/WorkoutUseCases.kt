package com.neyra.gymapp.domain.usecase

import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.repository.TrainingProgramRepository
import com.neyra.gymapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

        // Save the updated workout
        return workoutRepository.updateWorkout(workoutId, updatedWorkout)
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

        val existingExercise = exercises.map { it }.getOrNull()
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