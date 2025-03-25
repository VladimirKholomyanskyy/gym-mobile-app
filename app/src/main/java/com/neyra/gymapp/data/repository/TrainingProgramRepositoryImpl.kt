package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.domain.repository.TrainingProgramRepository
import com.neyra.gymapp.openapi.apis.TrainingProgramsApi
import com.neyra.gymapp.openapi.models.CreateTrainingProgramRequest
import com.neyra.gymapp.openapi.models.PatchTrainingProgramRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingProgramRepositoryImpl @Inject constructor(
    private val trainingProgramDao: TrainingProgramDao,
    private val trainingProgramsApi: TrainingProgramsApi,
    private val networkManager: NetworkManager
) : TrainingProgramRepository {

    override suspend fun createTrainingProgram(
        profileId: String,
        program: TrainingProgram
    ): Result<TrainingProgram> {
        return try {
            // Convert domain model to entity
            val programEntity = program.toEntity(profileId)

            // Perform local save
            trainingProgramDao.insert(programEntity)

            // If online, attempt to sync
            if (networkManager.isOnline()) {
                val response = trainingProgramsApi.createTrainingProgram(
                    CreateTrainingProgramRequest(
                        name = program.name,
                        description = program.description
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    // Update local entity with server response
                    trainingProgramDao.updateIdAndSyncStatus(
                        programEntity.id,
                        response.body()!!.id.toString(),
                        SyncStatus.SYNCED,
                        response.body()!!.createdAt.toInstant().toEpochMilli(),
                        response.body()!!.updatedAt.toInstant().toEpochMilli()
                    )
                }
            }

            // Return the domain model
            Result.success(program.copy(id = programEntity.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateFields(
        id: String,
        name: String?,
        description: String?
    ): Result<TrainingProgram> {
        return try {
            // Determine which update method to call based on provided parameters
            val rowsUpdated = when {
                name != null && description != null ->
                    trainingProgramDao.updateNameAndDescription(
                        id,
                        name,
                        description,
                        SyncStatus.PENDING_UPDATE,
                        System.currentTimeMillis()
                    )

                name != null ->
                    trainingProgramDao.updateName(
                        id,
                        name,
                        SyncStatus.PENDING_UPDATE,
                        System.currentTimeMillis()
                    )

                description != null ->
                    trainingProgramDao.updateDescription(
                        id,
                        description,
                        SyncStatus.PENDING_UPDATE,
                        System.currentTimeMillis()
                    )

                else -> 0 // No fields to update
            }

            if (rowsUpdated > 0) {
                // Fetch and return the updated entity
                val updatedProgram = trainingProgramDao.getById(id)
                    ?: return Result.failure(Exception("Training program not found after update"))
                if (networkManager.isOnline()) {
                    val response = trainingProgramsApi.updateTrainingProgram(
                        UUID.fromString(id),
                        PatchTrainingProgramRequest(
                            name = updatedProgram.name,
                            description = updatedProgram.description
                        )
                    )
                    if (response.isSuccessful && response.body() != null) {
                        // Mark as synced
                        trainingProgramDao.updateSyncStatus(
                            id, SyncStatus.SYNCED,
                            response.body()!!.updatedAt.toInstant().toEpochMilli()
                        )
                    }

                }
                Result.success(updatedProgram.toDomain())
            } else {
                Result.failure(Exception("No rows updated. Training program may not exist."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrainingProgram(programId: String): Result<Boolean> {
        return try {
            // Verify program exists
            trainingProgramDao.getById(programId)
                ?: throw IllegalArgumentException("Program not found")

            // Mark for deletion locally
            trainingProgramDao.updateSyncStatus(
                programId,
                SyncStatus.PENDING_DELETE
            )

            // If online, attempt to sync
            if (networkManager.isOnline()) {
                val apiResponse =
                    trainingProgramsApi.deleteTrainingProgram(UUID.fromString(programId))

                if (apiResponse.isSuccessful) {
                    // Permanent delete if sync successful
                    trainingProgramDao.delete(programId)
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrainingProgram(programId: String): TrainingProgram? {
        return trainingProgramDao.getById(programId)?.toDomain()
    }


    override fun getTrainingPrograms(
        profileId: String,
        sortBy: TrainingProgramRepository.SortCriteria,
        filterBy: TrainingProgramRepository.FilterCriteria?,
        limit: Int?
    ): Flow<List<TrainingProgram>> {
        return trainingProgramDao.getAllByProfileId(profileId)
            .map { entities ->
                // Apply filtering and sorting
                entities.map { it.toDomain() }
                    .let { programs ->
                        filterBy?.let { criteria ->
                            programs.filter { program ->
                                (criteria.minWorkouts == null || program.workoutCount >= criteria.minWorkouts) &&
                                        (criteria.maxWorkouts == null || program.workoutCount <= criteria.maxWorkouts) &&
                                        (criteria.nameContains == null || program.name.contains(
                                            criteria.nameContains,
                                            ignoreCase = true
                                        )) &&
                                        (criteria.complexity == null || program.getProgramComplexity() == criteria.complexity)
                            }
                        } ?: programs
                    }
                    .let { filteredPrograms ->
                        // Sort based on criteria
                        when (sortBy) {
                            TrainingProgramRepository.SortCriteria.CREATED_DATE ->
                                filteredPrograms.sortedByDescending { it.name }

                            TrainingProgramRepository.SortCriteria.NAME ->
                                filteredPrograms.sortedBy { it.name }

                            TrainingProgramRepository.SortCriteria.WORKOUT_COUNT ->
                                filteredPrograms.sortedByDescending { it.workoutCount }
                        }
                    }
                    .let { sortedPrograms ->
                        // Apply limit if specified
                        limit?.let { sortedPrograms.take(it) } ?: sortedPrograms
                    }
            }
    }


    override suspend fun trainingProgramExists(programId: String): Boolean {
        return trainingProgramDao.getById(programId) != null
    }

}