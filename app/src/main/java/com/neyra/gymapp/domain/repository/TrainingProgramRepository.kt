package com.neyra.gymapp.domain.repository


import com.neyra.gymapp.domain.model.TrainingProgram
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level repository interface for Training Programs
 *
 * Key characteristics:
 * - Uses domain models instead of entities
 * - Returns Result or Flow for robust error handling
 * - Defines core operations for training programs
 */
interface TrainingProgramRepository {
    /**
     * Create a new training program
     *
     * @param profileId ID of the profile creating the program
     * @param program The training program to create
     * @return Result containing the created training program
     */
    suspend fun createTrainingProgram(
        profileId: String,
        program: TrainingProgram
    ): Result<TrainingProgram>


    suspend fun updateFields(
        id: String,
        name: String? = null,
        description: String? = null
    ): Result<TrainingProgram>

    /**
     * Delete a training program
     *
     * @param programId ID of the program to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteTrainingProgram(programId: String): Result<Boolean>

    /**
     * Get a specific training program by ID
     *
     * @param programId ID of the program to retrieve
     * @return The training program or null if not found
     */
    suspend fun getTrainingProgram(programId: String): TrainingProgram?


    /**
     * Get training programs with additional filtering and sorting
     *
     * @param profileId ID of the profile
     * @param sortBy Sorting criteria
     * @param filterBy Filtering criteria
     * @param limit Maximum number of programs to retrieve
     * @return Flow of filtered and sorted training programs
     */
    fun getTrainingPrograms(
        profileId: String,
        sortBy: SortCriteria = SortCriteria.CREATED_DATE,
        filterBy: FilterCriteria? = null,
        limit: Int? = null
    ): Flow<List<TrainingProgram>>


    /**
     * Check if a training program exists
     *
     * @param programId ID of the program to check
     * @return Boolean indicating existence
     */
    suspend fun trainingProgramExists(programId: String): Boolean

    /**
     * Sorting criteria for training programs
     */
    enum class SortCriteria {
        CREATED_DATE,
        NAME,
        WORKOUT_COUNT
    }

    /**
     * Filtering criteria for training programs
     */
    data class FilterCriteria(
        val minWorkouts: Int? = null,
        val maxWorkouts: Int? = null,
        val nameContains: String? = null,
        val complexity: TrainingProgram.ProgramComplexity? = null
    )
}

/**
 * Extension function to provide a default implementation for getting programs
 */
fun TrainingProgramRepository.getTrainingPrograms(
    profileId: String,
    sortBy: TrainingProgramRepository.SortCriteria = TrainingProgramRepository.SortCriteria.CREATED_DATE
): Flow<List<TrainingProgram>> = getTrainingPrograms(profileId, sortBy)