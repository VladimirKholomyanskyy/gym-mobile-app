package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.data.mapper.toEntityList
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.TrainingProgramsApi
import com.neyra.gymapp.openapi.models.CreateTrainingProgramRequest
import com.neyra.gymapp.openapi.models.PatchTrainingProgramRequest
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingProgramRepository @Inject constructor(
    private val trainingProgramDao: TrainingProgramDao,
    private val trainingProgramsApi: TrainingProgramsApi,
    private val networkManager: NetworkManager
) {

    fun getTrainingPrograms(profileId: String): Flow<List<TrainingProgramEntity>> =
        trainingProgramDao.getAllByProfileId(profileId)

    fun getPendingSyncPrograms(): Flow<List<TrainingProgramEntity>> =
        trainingProgramDao.getAllWithSyncStatus(
            SyncStatus.PENDING_CREATE,
            SyncStatus.PENDING_UPDATE,
            SyncStatus.PENDING_DELETE
        )

    // Create program locally first, mark for sync
    suspend fun createTrainingProgram(
        profileId: UUID,
        name: String,
        description: String?
    ): Result<UUID> {
        val localId = UUID.randomUUID()
        val program = TrainingProgramEntity(
            id = localId,
            name = name,
            description = description ?: "",
            profileId = profileId,
            syncStatus = SyncStatus.PENDING_CREATE,
            lastModified = System.currentTimeMillis()
        )

        trainingProgramDao.insert(program)

        // Try to sync immediately if online
        if (networkManager.isOnline()) {
            try {
                syncProgram(program)
            } catch (e: Exception) {
                // Just log the error, we'll sync later
                // Logger.e("Failed to sync new program: ${e.message}")
            }
        }

        return Result.success(localId)
    }

    // Update locally first, mark for sync
    suspend fun updateTrainingProgram(
        programId: UUID,
        name: String?,
        description: String?
    ): Result<Boolean> {
        val currentProgram = trainingProgramDao.getById(programId) ?: return Result.failure(
            Exception("Program not found")
        )

        val updatedProgram = currentProgram.copy(
            name = name ?: currentProgram.name,
            description = description ?: currentProgram.description,
            syncStatus = if (currentProgram.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else currentProgram.syncStatus,
            lastModified = System.currentTimeMillis()
        )

        trainingProgramDao.insert(updatedProgram)

        // Try to sync immediately if online
        if (networkManager.isOnline()) {
            try {
                syncProgram(updatedProgram)
            } catch (e: Exception) {
                // Just log the error, we'll sync later
            }
        }

        return Result.success(true)
    }

    // Mark for deletion locally first, sync later
    suspend fun deleteTrainingProgram(programId: UUID): Result<Boolean> {
        val program = trainingProgramDao.getById(programId) ?: return Result.failure(
            Exception("Program not found")
        )

        // If it's a local-only entry that hasn't been synced yet, delete it immediately
        if (program.syncStatus == SyncStatus.PENDING_CREATE) {
            trainingProgramDao.delete(programId)
        } else {
            // Otherwise mark for deletion
            trainingProgramDao.updateSyncStatus(programId, SyncStatus.PENDING_DELETE)

            // Try to sync immediately if online
            if (networkManager.isOnline()) {
                try {
                    syncProgram(program.copy(syncStatus = SyncStatus.PENDING_DELETE))
                } catch (e: Exception) {
                    // Just log the error, we'll sync later
                }
            }
        }

        return Result.success(true)
    }

    // Main sync function to be called by WorkManager
    suspend fun syncAllPendingPrograms(): Result<Int> {
        if (!networkManager.isOnline()) {
            return Result.failure(Exception("No network connection"))
        }

        var syncCount = 0
        val pendingPrograms = trainingProgramDao.getAllWithSyncStatusSync(
            SyncStatus.PENDING_CREATE,
            SyncStatus.PENDING_UPDATE,
            SyncStatus.PENDING_DELETE
        )

        pendingPrograms.forEach { program ->
            try {
                val result = syncProgram(program)
                if (result.isSuccess) syncCount++
            } catch (e: Exception) {
                // Log but continue with other items
            }
        }

        return Result.success(syncCount)
    }

    private suspend fun syncProgram(program: TrainingProgramEntity): Result<Boolean> {
        return when (program.syncStatus) {
            SyncStatus.PENDING_CREATE -> createProgramRemote(program)
            SyncStatus.PENDING_UPDATE -> updateProgramRemote(program)
            SyncStatus.PENDING_DELETE -> deleteProgramRemote(program)
            else -> Result.success(false) // Nothing to sync
        }
    }

    private suspend fun createProgramRemote(program: TrainingProgramEntity): Result<Boolean> {
        try {
            val request =
                CreateTrainingProgramRequest(program.name, program.description.ifEmpty { null })
            val response = trainingProgramsApi.createTrainingProgram(request)

            if (response.isSuccessful && response.body() != null) {
                val serverProgram = response.body()!!
                // Update local ID with server ID and mark as synced
                trainingProgramDao.updateIdAndSyncStatus(
                    oldId = program.id,
                    newId = serverProgram.id,
                    syncStatus = SyncStatus.SYNCED,
                    lastModified = System.currentTimeMillis()
                )
                return Result.success(true)
            } else {
                return Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun updateProgramRemote(program: TrainingProgramEntity): Result<Boolean> {
        try {
            val request =
                PatchTrainingProgramRequest(program.name, program.description.ifEmpty { null })
            val response = trainingProgramsApi.updateTrainingProgram(program.id, request)

            if (response.isSuccessful) {
                // Mark as synced
                trainingProgramDao.updateSyncStatus(program.id, SyncStatus.SYNCED)
                return Result.success(true)
            } else {
                return Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private suspend fun deleteProgramRemote(program: TrainingProgramEntity): Result<Boolean> {
        try {
            val response = trainingProgramsApi.deleteTrainingProgram(program.id)

            if (response.isSuccessful) {
                // Actually delete from local DB now
                trainingProgramDao.delete(program.id)
                return Result.success(true)
            } else if (response.code() == 404) {
                // Already deleted on server, so delete locally too
                trainingProgramDao.delete(program.id)
                return Result.success(true)
            } else {
                return Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Fetch programs from API and update the local database
    suspend fun refreshTrainingPrograms(): Result<Int> {
        if (!networkManager.isOnline()) {
            return Result.failure(Exception("No network connection"))
        }

        return try {
            val response = trainingProgramsApi.listTrainingPrograms()
            if (response.isSuccessful && response.body() != null) {
                val apiPrograms = response.body()!!
                val entities = apiPrograms.items?.toEntityList() ?: emptyList()

                // Preserve local changes by only updating synced records
                val syncedEntities = entities.map { entity ->
                    val localEntity = trainingProgramDao.getById(entity.id)
                    if (localEntity != null && localEntity.syncStatus != SyncStatus.SYNCED) {
                        // Keep local version with pending sync status
                        localEntity
                    } else {
                        // Use server version but mark as synced
                        entity.copy(syncStatus = SyncStatus.SYNCED)
                    }
                }

                trainingProgramDao.upsertAll(syncedEntities)
                Result.success(syncedEntities.size)
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}