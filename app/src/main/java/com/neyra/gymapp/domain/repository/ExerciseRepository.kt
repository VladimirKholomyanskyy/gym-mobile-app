package com.neyra.gymapp.domain.repository

import com.neyra.gymapp.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level repository interface for Exercises
 */
interface ExerciseRepository {
    /**
     * Get a specific exercise by ID
     *
     * @param exerciseId ID of the exercise to retrieve
     * @return The exercise or null if not found
     */
    suspend fun getExerciseById(exerciseId: String): Exercise?

    /**
     * Get all exercises, optionally filtered and sorted
     *
     * @param filter Optional filter criteria (e.g., muscle group, equipment type)
     * @param sortBy Optional sort parameter
     * @return Flow of exercises
     */
    fun getExercises(
        filter: FilterCriteria? = null,
        sortBy: SortCriteria = SortCriteria.NAME
    ): Flow<List<Exercise>>

    /**
     * Search exercises by name
     *
     * @param query Search query string
     * @return Flow of exercises matching the query
     */
    fun searchExercises(query: String): Flow<List<Exercise>>

    /**
     * Refresh exercises from remote source
     *
     * @return Result indicating success or failure
     */
    suspend fun refreshExercises(): Result<Boolean>

    /**
     * Sort criteria for exercises
     */
    enum class SortCriteria {
        NAME,
        MUSCLE_GROUP,
        EQUIPMENT
    }

    /**
     * Filter criteria for exercises
     */
    data class FilterCriteria(
        val muscleGroup: String? = null,
        val equipment: String? = null,
        val difficulty: String? = null
    )
}