package com.neyra.gymapp.domain.repository

import com.neyra.gymapp.domain.model.Workout
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
     * Sorting criteria for workouts
     */
    enum class SortCriteria {
        POSITION,
        NAME,
        EXERCISE_COUNT
    }
}