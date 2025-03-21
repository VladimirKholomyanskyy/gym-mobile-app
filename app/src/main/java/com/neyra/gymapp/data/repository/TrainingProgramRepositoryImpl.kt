package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.domain.repository.TrainingProgramRepository
import com.neyra.gymapp.openapi.apis.TrainingProgramsApi
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
                val apiResponse = trainingProgramsApi.createTrainingProgram(
                    com.neyra.gymapp.openapi.models.CreateTrainingProgramRequest(
                        name = program.name,
                        description = program.description
                    )
                )

                if (apiResponse.isSuccessful) {
                    // Update local entity with server response
                    apiResponse.body()?.let { serverProgram ->
                        trainingProgramDao.updateIdAndSyncStatus(
                            oldId = programEntity.id,
                            newId = serverProgram.id.toString(),
                            syncStatus = SyncStatus.SYNCED,
                            lastModified = System.currentTimeMillis()
                        )
                    }
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
                    trainingProgramDao.updateNameAndDescription(id, name, description)

                name != null ->
                    trainingProgramDao.updateName(id, name)

                description != null ->
                    trainingProgramDao.updateDescription(id, description)

                else -> 0 // No fields to update
            }

            if (rowsUpdated > 0) {
                // Fetch and return the updated entity
                val updatedProgram = trainingProgramDao.getById(id)
                    ?: return Result.failure(Exception("Training program not found after update"))
                if (networkManager.isOnline()) {
                    val apiResponse = trainingProgramsApi.updateTrainingProgram(
                        UUID.fromString(id),
                        com.neyra.gymapp.openapi.models.PatchTrainingProgramRequest(
                            name = updatedProgram.name,
                            description = updatedProgram.description
                        )
                    )

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
                                filteredPrograms.sortedByDescending { it.createdAt }

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

    override suspend fun countTrainingPrograms(profileId: String): Int {
        return trainingProgramDao.countByProfileId(profileId)
    }

    override suspend fun trainingProgramExists(programId: String): Boolean {
        return trainingProgramDao.getById(programId.toString()) != null
    }

    override suspend fun syncTrainingPrograms(profileId: String): Result<Int> {
        return try {
            // Fetch pending sync items
            val pendingPrograms = trainingProgramDao.getAllWithSyncStatusSync(
                SyncStatus.PENDING_CREATE,
                SyncStatus.PENDING_UPDATE,
                SyncStatus.PENDING_DELETE
            )

            var syncedCount = 0
            pendingPrograms.forEach { program ->
                try {
                    when (program.syncStatus) {
                        SyncStatus.PENDING_CREATE -> {
                            val apiResponse = trainingProgramsApi.createTrainingProgram(
                                com.neyra.gymapp.openapi.models.CreateTrainingProgramRequest(
                                    name = program.name,
                                    description = program.description.ifEmpty { null }
                                )
                            )
                            if (apiResponse.isSuccessful) syncedCount++
                        }

                        SyncStatus.PENDING_UPDATE -> {
                            val apiResponse = trainingProgramsApi.updateTrainingProgram(
                                UUID.fromString(program.id),
                                com.neyra.gymapp.openapi.models.PatchTrainingProgramRequest(
                                    name = program.name,
                                    description = program.description.ifEmpty { null }
                                )
                            )
                            if (apiResponse.isSuccessful) syncedCount++
                        }

                        SyncStatus.PENDING_DELETE -> {
                            val apiResponse =
                                trainingProgramsApi.deleteTrainingProgram(UUID.fromString(program.id))
                            if (apiResponse.isSuccessful) syncedCount++
                        }

                        else -> {} // No action for already synced items
                    }
                } catch (e: Exception) {
                    // Log error but continue with other items
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}