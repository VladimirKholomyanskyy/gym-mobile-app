package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.error.runDomainCatching
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.model.toDomain
import com.neyra.gymapp.domain.repository.WorkoutExerciseRepository
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import com.neyra.gymapp.openapi.models.CreateWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.PatchWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.ReorderWorkoutExerciseRequest
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

@Singleton
class WorkoutExerciseRepositoryImpl @Inject constructor(
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutExerciseApi: WorkoutExercisesApi,
    private val networkManager: NetworkManager
) : WorkoutExerciseRepository {

    // Shared flow for triggering workout exercise refreshes
    private val refreshTrigger = MutableSharedFlow<String>(replay = 1)

    override suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Result<WorkoutExercise> =
        runDomainCatching {
            Timber.d("Adding exercise to workout: ${workoutExercise.workoutId}")

            // Validate exercise
            validateWorkoutExercise(workoutExercise)

            // Get the next position if not specified
            val position = if (workoutExercise.position <= 0) {
                (workoutExerciseDao.getMaxPosition(workoutExercise.workoutId) ?: -1) + 1
            } else {
                workoutExercise.position
            }

            // Convert to entity with correct position
            val exerciseWithPosition = workoutExercise.copy(position = position)
            val entity = exerciseWithPosition.toEntity()

            // Save locally
            workoutExerciseDao.insert(entity)
            Timber.d("Added exercise locally with ID: ${entity.id}")

            // If online, sync with server
            if (networkManager.isOnline()) {
                try {
                    val request = CreateWorkoutExerciseRequest(
                        workoutId = UUID.fromString(workoutExercise.workoutId),
                        exerciseId = UUID.fromString(workoutExercise.exercise.id),
                        sets = workoutExercise.sets,
                        reps = workoutExercise.reps
                    )

                    val response = workoutExerciseApi.addWorkoutExercise(request)

                    if (response.isSuccessful && response.body() != null) {
                        // Update local entity with server ID and sync status
                        val remoteResponse = response.body()!!
                        val remoteId = remoteResponse.id.toString()

                        workoutExerciseDao.updateIdAndSyncStatus(
                            entity.id,
                            remoteId,
                            SyncStatus.SYNCED
                        )

                        // Refresh the workout exercise list
                        refreshTrigger.emit(workoutExercise.workoutId)

                        // Return with the remote ID
                        return@runDomainCatching exerciseWithPosition.copy(id = remoteId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing workout exercise creation")
                    // Continue with local entity
                }
            }

            // Return local entity
            exerciseWithPosition.copy(id = entity.id)
        }

    override suspend fun updateWorkoutExercise(
        workoutExerciseId: String,
        workoutExercise: WorkoutExercise
    ): Result<WorkoutExercise> = runDomainCatching {
        Timber.d("Updating workout exercise: $workoutExerciseId")

        // Validate exercise
        validateWorkoutExercise(workoutExercise)

        // Get the entity to update
        val existingEntity = workoutExerciseDao.getById(workoutExerciseId)
            ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "WorkoutExercise")
                .withContext("id", workoutExerciseId)

        // Update the entity
        val updatedEntity = existingEntity.copy(
            sets = workoutExercise.sets,
            reps = workoutExercise.reps,
            syncStatus = SyncStatus.PENDING_UPDATE
        )

        workoutExerciseDao.insert(updatedEntity)
        Timber.d("Updated workout exercise locally")

        // If online, sync with server
        if (networkManager.isOnline()) {
            try {
                val request = PatchWorkoutExerciseRequest(
                    sets = workoutExercise.sets,
                    reps = workoutExercise.reps
                )

                val response = workoutExerciseApi.updateWorkoutExercise(
                    UUID.fromString(workoutExerciseId),
                    request
                )

                if (response.isSuccessful) {
                    // Update sync status
                    workoutExerciseDao.updateSyncStatus(workoutExerciseId, SyncStatus.SYNCED)

                    // Refresh the workout exercise list
                    refreshTrigger.emit(workoutExercise.workoutId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing workout exercise update")
                // Continue with local update
            }
        }

        // Get exercise details for the domain model
        val exercise = exerciseDao.getExerciseById(updatedEntity.exerciseId)
            ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "Exercise")
                .withContext("id", updatedEntity.exerciseId)

        // Return domain model
        updatedEntity.toDomain(exercise.toDomain())
    }

    override suspend fun deleteWorkoutExercise(workoutExerciseId: String): Result<Boolean> =
        runDomainCatching {
            Timber.d("Deleting workout exercise: $workoutExerciseId")

            // Get the entity to delete
            val entity = workoutExerciseDao.getById(workoutExerciseId)
                ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "WorkoutExercise")
                    .withContext("id", workoutExerciseId)

            val workoutId = entity.workoutId

            // Delete locally
            workoutExerciseDao.deleteById(workoutExerciseId)
            Timber.d("Deleted workout exercise locally")

            // If online, sync with server
            if (networkManager.isOnline()) {
                try {
                    val response = workoutExerciseApi.deleteWorkoutExercise(
                        UUID.fromString(workoutExerciseId)
                    )

                    if (response.isSuccessful) {
                        // Refresh the workout exercise list
                        refreshTrigger.emit(workoutId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing workout exercise deletion")
                    // Continue with local deletion
                }
            }

            true
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getWorkoutExercises(workoutId: String): Flow<List<WorkoutExercise>> {
        // Emit initial value to trigger first load
        refreshTrigger.tryEmit(workoutId)

        // Use flatMapLatest to efficiently handle refreshes
        return refreshTrigger.flatMapLatest { id ->
            workoutExerciseDao.getAllByWorkoutId(id)
                .catch { e ->
                    Timber.e(e, "Error loading workout exercises for workout: $id")
                    throw e
                }
                .map { entities ->
                    entities.mapNotNull { entity ->
                        try {
                            val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                            if (exercise != null) {
                                entity.toDomain(exercise.toDomain())
                            } else {
                                Timber.w("Exercise not found for ID: ${entity.exerciseId}")
                                null
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error mapping workout exercise entity to domain model")
                            null
                        }
                    }.sortedBy { it.position }
                }
        }
    }

    override suspend fun reorderWorkoutExercise(
        workoutExerciseId: String,
        newPosition: Int
    ): Result<Boolean> = runDomainCatching {
        Timber.d("Reordering workout exercise: $workoutExerciseId to position $newPosition")

        // Validate position
        if (newPosition < 0) {
            throw DomainError.ValidationError.InvalidName(
                "Position",
                "Position cannot be negative"
            )
        }

        // Get the entity to reorder
        val entity = workoutExerciseDao.getById(workoutExerciseId)
            ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "WorkoutExercise")
                .withContext("id", workoutExerciseId)

        // Update position locally
        workoutExerciseDao.reorderExercises(workoutExerciseId, newPosition)
        Timber.d("Reordered workout exercise locally")

        // If online, sync with server
        if (networkManager.isOnline()) {
            try {
                val request = ReorderWorkoutExerciseRequest(
                    position = newPosition
                )

                val response = workoutExerciseApi.reorderWorkoutExercise(
                    UUID.fromString(workoutExerciseId),
                    request
                )

                if (response.isSuccessful) {
                    // Refresh the workout exercise list
                    refreshTrigger.emit(entity.workoutId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing workout exercise reordering")
                // Continue with local reordering
            }
        }

        true
    }

    /**
     * Validates a workout exercise before saving
     */
    private fun validateWorkoutExercise(workoutExercise: WorkoutExercise) {
        // Validate sets
        if (workoutExercise.sets <= 0) {
            throw DomainError.WorkoutError.InvalidSetsOrReps(workoutExercise.sets)
                .withContext("parameter", "sets")
                .withContext("value", workoutExercise.sets)
                .withContext("message", "Sets must be greater than 0")
        }

        // Validate reps
        if (workoutExercise.reps <= 0) {
            throw DomainError.WorkoutError.InvalidSetsOrReps(workoutExercise.reps)
                .withContext("parameter", "reps")
                .withContext("value", workoutExercise.reps)
                .withContext("message", "Reps must be greater than 0")
        }
    }
}