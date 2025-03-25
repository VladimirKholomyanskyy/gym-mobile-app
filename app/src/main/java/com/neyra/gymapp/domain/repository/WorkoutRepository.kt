package com.neyra.gymapp.domain.repository

import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level repository interface for Workouts
 */
interface WorkoutRepository {
    /**
     * Create a new workout
     *
     * @param workout The workout to create
     * @return Result containing the created workout
     */
    suspend fun createWorkout(workout: Workout): Result<Workout>

    /**
     * Update an existing workout
     *
     * @param workoutId ID of the workout to update
     * @param newName Updated name
     * @return Result containing the updated workout
     */
    suspend fun updateWorkout(workoutId: String, newName: String): Result<Workout>

    /**
     * Delete a workout
     *
     * @param workoutId ID of the workout to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteWorkout(workoutId: String): Result<Boolean>

    /**
     * Get a specific workout by ID
     *
     * @param workoutId ID of the workout to retrieve
     * @return The workout or null if not found
     */
    suspend fun getWorkout(workoutId: String): Workout?

    /**
     * Get workouts for a specific training program
     *
     * @param trainingProgramId ID of the training program
     * @param sortBy Sorting criteria
     * @return Flow of workouts for the specified program
     */
    fun getWorkouts(
        trainingProgramId: String,
        sortBy: SortCriteria = SortCriteria.POSITION
    ): Flow<List<Workout>>

    /**
     * Reorder a workout within its training program
     *
     * @param workoutId ID of the workout to reorder
     * @param newPosition New position for the workout
     * @return Result indicating success or failure
     */
    suspend fun reorderWorkout(workoutId: String, newPosition: Int): Result<Boolean>

    /**
     * Add an exercise to a workout
     *
     * @param workoutExercise The workout exercise to add
     * @return Result containing the added workout exercise
     */
    suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Result<WorkoutExercise>

    /**
     * Update a workout exercise
     *
     * @param workoutExerciseId ID of the workout exercise to update
     * @param workoutExercise Updated workout exercise details
     * @return Result containing the updated workout exercise
     */
    suspend fun updateWorkoutExercise(
        workoutExerciseId: String,
        workoutExercise: WorkoutExercise
    ): Result<WorkoutExercise>

    /**
     * Delete a workout exercise
     *
     * @param workoutExerciseId ID of the workout exercise to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteWorkoutExercise(workoutExerciseId: String): Result<Boolean>

    /**
     * Get exercises for a specific workout
     *
     * @param workoutId ID of the workout
     * @return Flow of workout exercises for the specified workout
     */
    fun getWorkoutExercises(workoutId: String): Flow<List<WorkoutExercise>>

    /**
     * Reorder an exercise within a workout
     *
     * @param workoutExerciseId ID of the workout exercise to reorder
     * @param newPosition New position for the exercise
     * @return Result indicating success or failure
     */
    suspend fun reorderWorkoutExercise(
        workoutExerciseId: String,
        newPosition: Int
    ): Result<Boolean>

    /**
     * Sync workouts with remote data source
     *
     * @param trainingProgramId ID of the training program
     * @return Result of synchronization
     */
    suspend fun syncWorkouts(trainingProgramId: String): Result<Int>

    /**
     * Sorting criteria for workouts
     */
    enum class SortCriteria {
        POSITION,
        NAME,
        EXERCISE_COUNT,
        CREATED_DATE
    }
}