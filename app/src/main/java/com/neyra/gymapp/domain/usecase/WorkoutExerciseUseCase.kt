package com.neyra.gymapp.domain.usecase

import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.error.runDomainCatching
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.repository.ExerciseRepository
import com.neyra.gymapp.domain.repository.WorkoutExerciseRepository
import com.neyra.gymapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for adding an exercise to a workout
 */
class AddExerciseToWorkoutUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val workoutExerciseRepository: WorkoutExerciseRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(
        workoutId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ): Result<WorkoutExercise> = runDomainCatching {
        // Input validation
        validateInputs(sets, reps)

        // Check if workout exists
        val workout = workoutRepository.getWorkout(workoutId)
            ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "Workout")
                .withContext("id", workoutId)

        // Check if exercise exists
        val exercise = exerciseRepository.getExerciseById(exerciseId)
            ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "Exercise")
                .withContext("id", exerciseId)

        // Check if workout can have more exercises
        if (workout.exercises.size >= 15) {
            throw DomainError.ValidationError.WorkoutLimitExceeded()
                .withContext("workoutId", workoutId)
                .withContext("currentExercises", workout.exercises.size)
                .withContext("maxExercises", 15)
        }

        // Get the next position in the workout
        val position = workout.exercises.maxOfOrNull { it.position } ?: (0 + 1)

        // Create the workout exercise
        val workoutExercise = WorkoutExercise(
            workoutId = workoutId,
            exercise = exercise,
            sets = sets,
            reps = reps,
            position = position
        )

        // Add the exercise to workout through repository
        workoutExerciseRepository.addExerciseToWorkout(workoutExercise).getOrThrow()
    }

    private fun validateInputs(sets: Int, reps: Int) {
        if (sets <= 0) {
            throw DomainError.WorkoutError.InvalidSetsOrReps(sets)
                .withContext("parameter", "sets")
                .withContext("value", sets)
                .withContext("reason", "Sets must be greater than 0")
        }

        if (reps <= 0) {
            throw DomainError.WorkoutError.InvalidSetsOrReps(reps)
                .withContext("parameter", "reps")
                .withContext("value", reps)
                .withContext("reason", "Reps must be greater than 0")
        }
    }
}

class UpdateWorkoutExerciseUseCase @Inject constructor(
    private val workoutExerciseRepository: WorkoutExerciseRepository
) {
    suspend operator fun invoke(
        workoutExerciseId: String,
        sets: Int,
        reps: Int
    ): Result<WorkoutExercise> = runDomainCatching {
        // Input validation
        if (sets <= 0) {
            throw DomainError.WorkoutError.InvalidSetsOrReps(sets)
                .withContext("parameter", "sets")
                .withContext("value", sets)
        }

        if (reps <= 0) {
            throw DomainError.WorkoutError.InvalidSetsOrReps(reps)
                .withContext("parameter", "reps")
                .withContext("value", reps)
        }

        // Get current workout exercise - use first() instead of collect
        val exercise = workoutExerciseRepository.getWorkoutExercises(workoutExerciseId)
            .map { list -> list.find { it.id == workoutExerciseId } }
            .catch {
                Timber.e(it, "Error fetching workout exercise: $workoutExerciseId")
                throw DomainError.DataError.NotFound()
                    .withContext("entityType", "WorkoutExercise")
                    .withContext("id", workoutExerciseId)
            }
            .first()

        // Check if exercise exists
        if (exercise == null) {
            throw DomainError.DataError.NotFound()
                .withContext("entityType", "WorkoutExercise")
                .withContext("id", workoutExerciseId)
        }

        // Create updated exercise
        val updatedExercise = exercise.copy(sets = sets, reps = reps)

        // Update through repository
        workoutExerciseRepository.updateWorkoutExercise(
            workoutExerciseId,
            updatedExercise
        ).getOrThrow()
    }
}

/**
 * Use case for deleting a workout exercise
 */
class DeleteWorkoutExerciseUseCase @Inject constructor(
    private val workoutExerciseRepository: WorkoutExerciseRepository
) {
    suspend operator fun invoke(workoutExerciseId: String): Result<Boolean> = runDomainCatching {
        workoutExerciseRepository.deleteWorkoutExercise(workoutExerciseId).getOrThrow()
    }
}

/**
 * Use case for reordering a workout exercise
 */
class ReorderWorkoutExerciseUseCase @Inject constructor(
    private val workoutExerciseRepository: WorkoutExerciseRepository
) {
    suspend operator fun invoke(workoutExerciseId: String, newPosition: Int): Result<Boolean> =
        runDomainCatching {
            // Validate position
            if (newPosition < 0) {
                throw DomainError.ValidationError.InvalidName(
                    "Position",
                    "Position cannot be negative"
                )
            }

            // Reorder through repository
            workoutExerciseRepository.reorderWorkoutExercise(workoutExerciseId, newPosition)
                .getOrThrow()
        }
}

/**
 * Use case for getting workout exercises
 */
class GetWorkoutExercisesUseCase @Inject constructor(
    private val workoutExerciseRepository: WorkoutExerciseRepository
) {
    operator fun invoke(workoutId: String): Flow<List<WorkoutExercise>> {
        return workoutExerciseRepository.getWorkoutExercises(workoutId)
    }
}