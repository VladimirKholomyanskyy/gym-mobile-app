package com.neyra.gymapp.domain.repository

import com.neyra.gymapp.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow

interface WorkoutExerciseRepository {
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
}