package com.neyra.gymapp.domain.usecase

import com.neyra.gymapp.data.repository.TrainingProgramRepositoryImpl
import com.neyra.gymapp.domain.model.TrainingProgram
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for creating a new training program
 */
class CreateTrainingProgramUseCase @Inject constructor(
    private val repository: TrainingProgramRepositoryImpl
) {
    suspend operator fun invoke(
        profileId: String,
        name: String,
        description: String? = null
    ): Result<TrainingProgram> {
        return try {
            // Create domain model with validation
            val trainingProgram = TrainingProgram(
                name = name,
                description = description
            )

            // Convert to entity and save
            val result = repository.createTrainingProgram(
                profileId,
                TrainingProgram(
                    name = trainingProgram.name,
                    description = trainingProgram.description
                )
            )

            // Map result back to domain model
            result.map { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for updating an existing training program
 */
class UpdateTrainingProgramUseCase @Inject constructor(
    private val repository: TrainingProgramRepositoryImpl
) {
    suspend operator fun invoke(
        programId: String,
        name: String? = null,
        description: String? = null
    ): Result<TrainingProgram> {
        return try {
            // Fetch existing program to validate updates
            repository.getTrainingProgram(programId)
                ?: throw IllegalArgumentException("Program not found")

            // Update via repository
            val result = repository.updateFields(
                programId,
                name,
                description
            )

            // Map result back to domain model
            result.map { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for deleting a training program
 */
class DeleteTrainingProgramUseCase @Inject constructor(
    private val repository: TrainingProgramRepositoryImpl
) {
    suspend operator fun invoke(programId: String): Result<Boolean> {
        return try {
            // Validate program exists before deletion
            repository.getTrainingProgram(programId)
                ?: throw IllegalArgumentException("Program not found")

            // Perform deletion
            repository.deleteTrainingProgram(programId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case for getting training programs
 */
class GetTrainingProgramsUseCase @Inject constructor(
    private val repository: TrainingProgramRepositoryImpl
) {
    // Get all training programs for a profile
    operator fun invoke(profileId: String): Flow<List<TrainingProgram>> {
        return repository.getTrainingPrograms(profileId)
            .map { entities ->
                entities.map { it }
            }
    }

    // Get a specific training program
    suspend fun getById(programId: String): TrainingProgram? {
        return repository.getTrainingProgram(programId)
    }
}

/**
 * Use case for getting training program complexity
 */
class GetProgramComplexityUseCase @Inject constructor(
    private val getTrainingProgramsUseCase: GetTrainingProgramsUseCase
) {
    suspend fun getComplexity(programId: String): TrainingProgram.ProgramComplexity? {
        return getTrainingProgramsUseCase.getById(programId)
            ?.getProgramComplexity()
    }
}