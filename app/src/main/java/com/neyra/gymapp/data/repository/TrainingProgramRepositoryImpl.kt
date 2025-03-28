package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.error.runDomainCatching
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.domain.repository.TrainingProgramRepository
import com.neyra.gymapp.openapi.apis.TrainingProgramsApi
import com.neyra.gymapp.openapi.models.CreateTrainingProgramRequest
import com.neyra.gymapp.openapi.models.PatchTrainingProgramRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the TrainingProgramRepository that handles CRUD operations
 * for training programs with local persistence and remote synchronization.
 */
@Singleton
class TrainingProgramRepositoryImpl @Inject constructor(
    private val trainingProgramDao: TrainingProgramDao,
    private val trainingProgramsApi: TrainingProgramsApi,
    private val networkManager: NetworkManager
) : TrainingProgramRepository {

    // Shared flow for triggering training program refreshes
    private val refreshTrigger = MutableSharedFlow<String>(replay = 1)

    /**
     * Implementation of refreshTrainingPrograms method to trigger a refresh of training programs
     */
    override suspend fun refreshTrainingPrograms(profileId: String): Result<Boolean> =
        runDomainCatching {
            if (!networkManager.isOnline()) {
                throw DomainError.NetworkError.NoConnection()
                    .withContext("operation", "refreshTrainingPrograms")
            }

            try {
                val response = trainingProgramsApi.listTrainingPrograms()

                if (response.isSuccessful && response.body() != null) {
                    val programs = response.body()!!.items ?: emptyList()

                    // Convert API models to entities and save to database
                    val entities = programs.map { program ->
                        val entity = TrainingProgramEntity(
                            id = program.id.toString(),
                            name = program.name,
                            description = program.description ?: "",
                            profileId = profileId,
                            localCreatedAt = program.createdAt.toInstant().toEpochMilli(),
                            localUpdatedAt = program.updatedAt.toInstant().toEpochMilli(),
                            serverCreatedAt = program.createdAt.toInstant().toEpochMilli(),
                            serverUpdatedAt = program.updatedAt.toInstant().toEpochMilli(),
                            syncStatus = SyncStatus.SYNCED
                        )
                        entity
                    }

                    trainingProgramDao.insertAll(entities)

                    // Trigger a refresh of the flow
                    refreshTrigger.emit(profileId)

                    return@runDomainCatching true
                } else {
                    throw DomainError.NetworkError.ServerError(
                        statusCode = response.code(),
                        message = "Failed to fetch training programs: ${
                            response.errorBody()?.string()
                        }"
                    )
                }
            } catch (e: Exception) {
                if (e is DomainError) throw e

                Timber.e(e, "Error refreshing training programs")
                throw DomainError.NetworkError.ServerError(
                    statusCode = 500,
                    message = "Failed to refresh training programs: ${e.message}"
                )
            }
        }

    /**
     * Creates a new training program both locally and remotely if network is available.
     *
     * @param profileId The ID of the user profile creating the program
     * @param program The training program domain model to create
     * @return Result containing the created training program or a domain error
     */
    override suspend fun createTrainingProgram(
        profileId: String,
        program: TrainingProgram
    ): Result<TrainingProgram> = runDomainCatching {
        Timber.d("Creating training program: ${program.name} for profile: $profileId")

        // Validate the program
        validateProgram(program)

        // Convert domain model to entity
        val programEntity = program.toEntity(profileId)

        // Perform local save
        trainingProgramDao.insert(programEntity)
        Timber.d("Saved training program locally with ID: ${programEntity.id}")

        // If online, attempt to sync
        if (networkManager.isOnline()) {
            try {
                val response = trainingProgramsApi.createTrainingProgram(
                    CreateTrainingProgramRequest(
                        name = program.name,
                        description = program.description
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val remoteId = response.body()!!.id.toString()
                    Timber.d("Program synced successfully with remote ID: $remoteId")

                    // Update local entity with server response
                    trainingProgramDao.updateIdAndSyncStatus(
                        programEntity.id,
                        remoteId,
                        SyncStatus.SYNCED,
                        response.body()!!.createdAt.toInstant().toEpochMilli(),
                        response.body()!!.updatedAt.toInstant().toEpochMilli()
                    )

                    // Trigger a refresh
                    refreshTrigger.emit(profileId)

                    // Return the domain model with updated ID
                    return@runDomainCatching program.copy(id = remoteId)
                } else {
                    val statusCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.w("Server responded with error $statusCode: $errorBody")
                    throw DomainError.fromHttpStatus(statusCode, errorBody)
                        .withContext("programName", program.name)
                        .withContext("profileId", profileId)
                }
            } catch (e: Exception) {
                if (e is DomainError) throw e

                Timber.e(e, "Network error while syncing program")
                // Continue with local save if network operation failed
                Timber.d("Continuing with local entity despite sync failure")
            }
        } else {
            Timber.d("No network connection, skipping remote sync")
        }

        // Return the domain model with the local ID
        program.copy(id = programEntity.id)
    }

    /**
     * Updates specific fields of a training program.
     *
     * @param id The ID of the program to update
     * @param name Optional new name for the program
     * @param description Optional new description for the program
     * @return Result containing the updated training program or a domain error
     */
    override suspend fun updateFields(
        id: String,
        name: String?,
        description: String?
    ): Result<TrainingProgram> = runDomainCatching {
        Timber.d("Updating program $id - name: ${name ?: "unchanged"}, description: ${if (description != null) "updated" else "unchanged"}")

        // Verify program exists
        val existingProgram = trainingProgramDao.getById(id)
            ?: throw DomainError.DataError.NotFound.INSTANCE
                .withContext("entityType", "TrainingProgram")
                .withContext("id", id)

        // Validate the updated fields
        name?.let {
            if (it.isBlank()) {
                throw DomainError.ValidationError.InvalidName(it)
                    .withContext("programId", id)
            }
            if (it.length > TrainingProgram.MAX_NAME_LENGTH) {
                throw DomainError.ValidationError.InvalidName(
                    it,
                    "Name exceeds maximum length of ${TrainingProgram.MAX_NAME_LENGTH} characters"
                )
                    .withContext("programId", id)
            }
        }

        description?.let {
            if (it.length > TrainingProgram.MAX_DESCRIPTION_LENGTH) {
                throw DomainError.ValidationError.InvalidDescription(
                    it,
                    "Description exceeds maximum length of ${TrainingProgram.MAX_DESCRIPTION_LENGTH} characters"
                )
                    .withContext("programId", id)
            }
        }

        // Determine which update method to call based on provided parameters
        val rowsUpdated = when {
            name != null && description != null -> {
                trainingProgramDao.updateNameAndDescription(
                    id,
                    name,
                    description,
                    SyncStatus.PENDING_UPDATE,
                    System.currentTimeMillis()
                )
            }

            name != null -> {
                trainingProgramDao.updateName(
                    id,
                    name,
                    SyncStatus.PENDING_UPDATE,
                    System.currentTimeMillis()
                )
            }

            description != null -> {
                trainingProgramDao.updateDescription(
                    id,
                    description,
                    SyncStatus.PENDING_UPDATE,
                    System.currentTimeMillis()
                )
            }

            else -> 0 // No fields to update
        }

        if (rowsUpdated <= 0) {
            Timber.w("No rows updated for program $id")
            throw DomainError.DataError.DatabaseError.INSTANCE
                .withContext("operation", "update")
                .withContext("programId", id)
        }

        // Fetch the updated entity
        val updatedProgram = trainingProgramDao.getById(id)
            ?: throw DomainError.DataError.NotFound.INSTANCE
                .withContext("entityType", "TrainingProgram")
                .withContext("id", id)

        // If online, synchronize with server
        if (networkManager.isOnline()) {
            Timber.d("Syncing updated program with server")
            try {
                val response = trainingProgramsApi.updateTrainingProgram(
                    UUID.fromString(id),
                    PatchTrainingProgramRequest(
                        name = updatedProgram.name,
                        description = updatedProgram.description
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    Timber.d("Program updated successfully on server")
                    // Mark as synced
                    trainingProgramDao.updateSyncStatus(
                        id,
                        SyncStatus.SYNCED,
                        response.body()!!.updatedAt.toInstant().toEpochMilli()
                    )

                    // Trigger a refresh
                    refreshTrigger.emit(updatedProgram.profileId)
                } else {
                    val statusCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.w("Server responded with error $statusCode: $errorBody")
                    // We don't throw here - local update was successful
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing program update with server")
                // We continue with local update if remote sync fails
            }
        } else {
            Timber.d("No network connection, skipping remote sync")
        }

        // Return the domain model
        updatedProgram.toDomain()
    }

    /**
     * Deletes a training program.
     *
     * @param programId The ID of the program to delete
     * @return Result indicating success or a domain error
     */
    override suspend fun deleteTrainingProgram(programId: String): Result<Boolean> =
        runDomainCatching {
            Timber.d("Deleting training program: $programId")

            // Verify program exists
            val program = trainingProgramDao.getById(programId)
                ?: throw DomainError.DataError.NotFound.INSTANCE
                    .withContext("entityType", "TrainingProgram")
                    .withContext("id", programId)

            // Store program ID for refresh
            val profileId = program.profileId

            // Mark for deletion locally
            trainingProgramDao.updateSyncStatus(
                programId,
                SyncStatus.PENDING_DELETE
            )
            Timber.d("Marked program for deletion locally")

            // If online, attempt to sync with server
            if (networkManager.isOnline()) {
                Timber.d("Network available, syncing deletion with server")
                try {
                    val apiResponse =
                        trainingProgramsApi.deleteTrainingProgram(UUID.fromString(programId))

                    if (apiResponse.isSuccessful) {
                        Timber.d("Program deleted successfully on server")
                        // Permanent delete if sync successful
                        trainingProgramDao.delete(programId)
                    } else {
                        val statusCode = apiResponse.code()
                        val errorBody = apiResponse.errorBody()?.string() ?: "Unknown error"
                        Timber.w("Server responded with error $statusCode: $errorBody")
                        // We don't throw here since local deletion was successful
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing program deletion with server")
                    // We continue with local deletion marked as pending
                }
            } else {
                Timber.d("No network connection, deletion will be synced later")
            }

            // Trigger a refresh
            refreshTrigger.emit(profileId)

            true
        }

    /**
     * Retrieves a training program by ID.
     *
     * @param programId The ID of the program to retrieve
     * @return The training program domain model or null if not found
     */
    override suspend fun getTrainingProgram(programId: String): TrainingProgram? {
        Timber.d("Getting training program: $programId")
        val entity = trainingProgramDao.getById(programId)

        if (entity == null) {
            Timber.d("Training program not found: $programId")
            return null
        }

        Timber.d("Found training program: ${entity.name}")
        return entity.toDomain()
    }

    /**
     * Gets a flow of training programs, optionally filtered and sorted.
     *
     * @param profileId The ID of the user profile
     * @param sortBy Criteria for sorting the results
     * @param filterBy Optional filtering criteria
     * @param limit Optional maximum number of results to return
     * @return Flow of training program domain models
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTrainingPrograms(
        profileId: String,
        sortBy: TrainingProgramRepository.SortCriteria,
        filterBy: TrainingProgramRepository.FilterCriteria?,
        limit: Int?
    ): Flow<List<TrainingProgram>> {
        Timber.d("Getting training programs for profile: $profileId")
        Timber.d("Sort by: $sortBy, Filter: $filterBy, Limit: $limit")

        // Trigger refresh for this profile ID
        refreshTrigger.tryEmit(profileId)

        // Use flatMapLatest to efficiently handle refreshes
        return refreshTrigger.flatMapLatest { pId ->
            if (pId != profileId) {
                // If the refreshed profile ID doesn't match, skip and just use the profile ID
                refreshTrigger.emit(profileId)
            }

            trainingProgramDao.getAllByProfileId(profileId)
                .map { entities ->
                    // Map entities to domain models
                    entities.map { it.toDomain() }
                }
                .map { programs ->
                    // Apply filtering if criteria provided
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
                .map { filteredPrograms ->
                    // Sort based on criteria
                    when (sortBy) {
                        TrainingProgramRepository.SortCriteria.NAME ->
                            filteredPrograms.sortedBy { it.name }

                        TrainingProgramRepository.SortCriteria.WORKOUT_COUNT ->
                            filteredPrograms.sortedByDescending { it.workoutCount }

                        TrainingProgramRepository.SortCriteria.RECENTLY_CREATED ->
                            filteredPrograms.sortedByDescending { it.id }

                        TrainingProgramRepository.SortCriteria.RECENTLY_UPDATED ->
                            filteredPrograms.sortedByDescending { it.id }
                    }
                }
                .map { sortedPrograms ->
                    // Apply limit if specified
                    limit?.let { sortedPrograms.take(it) } ?: sortedPrograms
                }
                .catch { exception ->
                    Timber.e(exception, "Error getting training programs")
                    throw exception
                }
        }
    }

    /**
     * Checks if a training program exists.
     *
     * @param programId The ID of the program to check
     * @return True if the program exists, false otherwise
     */
    override suspend fun trainingProgramExists(programId: String): Boolean {
        val exists = trainingProgramDao.getById(programId) != null
        Timber.d("Training program $programId exists: $exists")
        return exists
    }

    /**
     * Validates a training program before saving it.
     * Throws appropriate domain errors if validation fails.
     */
    private fun validateProgram(program: TrainingProgram) {
        // Name validation
        if (program.name.isBlank()) {
            throw DomainError.ValidationError.InvalidName(
                program.name,
                "Program name cannot be empty"
            )
        }

        if (program.name.length > TrainingProgram.MAX_NAME_LENGTH) {
            throw DomainError.ValidationError.InvalidName(
                program.name,
                "Program name cannot exceed ${TrainingProgram.MAX_NAME_LENGTH} characters"
            )
        }

        // Description validation (if present)
        program.description?.let {
            if (it.length > TrainingProgram.MAX_DESCRIPTION_LENGTH) {
                throw DomainError.ValidationError.InvalidDescription(
                    it,
                    "Description cannot exceed ${TrainingProgram.MAX_DESCRIPTION_LENGTH} characters"
                )
            }
        }
    }
}