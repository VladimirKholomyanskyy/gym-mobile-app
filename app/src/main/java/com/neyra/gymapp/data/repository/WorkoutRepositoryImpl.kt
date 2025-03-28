package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.error.runDomainCatching
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.repository.WorkoutRepository
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import com.neyra.gymapp.openapi.models.CreateWorkoutRequest
import com.neyra.gymapp.openapi.models.PatchWorkoutRequest
import com.neyra.gymapp.openapi.models.ReorderWorkoutRequest
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
 * Implementation of the WorkoutRepository with improved caching and error handling
 */
@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutsApi: WorkoutsApi,
    private val networkManager: NetworkManager
) : WorkoutRepository {

    // Shared flow for triggering workout refreshes
    private val refreshTrigger = MutableSharedFlow<String>(replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getWorkouts(
        trainingProgramId: String,
        sortBy: WorkoutRepository.SortCriteria
    ): Flow<List<Workout>> {
        // Emit initial value to trigger first load
        refreshTrigger.tryEmit(trainingProgramId)

        // Use flatMapLatest to efficiently handle refreshes
        return refreshTrigger.flatMapLatest { programId ->
            workoutDao.getWorkoutsByTrainingProgramId(programId)
                .map { workoutEntities ->
                    // Map each workout entity to domain model with exercises
                    workoutEntities.mapNotNull { workoutEntity ->
                        try {
                            // Get exercises for this workout
                            val exercises = mutableListOf<WorkoutExercise>()

                            // Need to collect the Flow to get the exercises
                            workoutExerciseDao.getAllByWorkoutId(workoutEntity.id)
                                .catch { e ->
                                    Timber.e(
                                        e,
                                        "Error loading exercises for workout ${workoutEntity.id}"
                                    )
                                }
                                .collect { entities ->
                                    entities.forEach { entity ->
                                        val exercise =
                                            exerciseDao.getExerciseById(entity.exerciseId)
                                        if (exercise != null) {
                                            exercises.add(entity.toDomain(exercise))
                                        }
                                    }
                                }

                            // Create domain model
                            workoutEntity.toDomain(exercises)
                        } catch (e: Exception) {
                            Timber.e(e, "Error mapping workout entity to domain model")
                            null
                        }
                    }.sortedWith(getSortComparator(sortBy))
                }
        }
    }

    override suspend fun createWorkout(workout: Workout): Result<Workout> = runDomainCatching {
        // Validate workout
        validateWorkout(workout)

        // Get the next position if not specified
        val position = if (workout.position <= 0) {
            (workoutDao.getMaxPosition(workout.trainingProgramId) ?: -1) + 1
        } else {
            workout.position
        }

        // Convert to entity with correct position
        val workoutWithPosition = workout.copy(position = position)
        val workoutEntity = workoutWithPosition.toEntity()

        // Save locally
        workoutDao.insert(workoutEntity)
        Timber.d("Created local workout: ${workoutEntity.name}")

        // Try to sync with server if online
        if (networkManager.isOnline()) {
            try {
                val request = CreateWorkoutRequest(
                    name = workoutEntity.name
                )

                val response = workoutsApi.addWorkoutToProgram(
                    UUID.fromString(workout.trainingProgramId),
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    // Update local entity with server ID and sync status
                    val remoteWorkout = response.body()!!
                    val remoteId = remoteWorkout.id.toString()
                    val createdAt = remoteWorkout.createdAt.toInstant().toEpochMilli()
                    val updatedAt = remoteWorkout.updatedAt.toInstant().toEpochMilli()

                    workoutDao.updateIdAndSyncStatus(
                        workoutEntity.id,
                        remoteId,
                        SyncStatus.SYNCED,
                        createdAt,
                        updatedAt
                    )

                    // Refresh workout list
                    refreshTrigger.emit(workout.trainingProgramId)

                    return@runDomainCatching workoutWithPosition.copy(id = remoteId)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing workout creation")
                // Continue with local entity
            }
        }

        // Return local entity
        workoutWithPosition.copy(id = workoutEntity.id)
    }

    override suspend fun updateWorkout(workoutId: String, newName: String): Result<Workout> =
        runDomainCatching {
            // Validate name
            if (newName.isBlank() || newName.length > Workout.MAX_NAME_LENGTH) {
                throw DomainError.ValidationError.InvalidName(
                    newName,
                    "Workout name must be between 1 and ${Workout.MAX_NAME_LENGTH} characters"
                )
            }

            // Get workout entity
            val workoutEntity =
                workoutDao.getWorkoutById(workoutId) ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Workout")
                    .withContext("id", workoutId)

            // Update locally
            val rowsUpdated = workoutDao.updateName(
                workoutId,
                newName,
                SyncStatus.PENDING_UPDATE,
                System.currentTimeMillis()
            )

            if (rowsUpdated <= 0) {
                throw DomainError.DataError.DatabaseError()
                    .withContext("operation", "updateWorkout")
                    .withContext("workoutId", workoutId)
            }

            // Get updated entity
            val updatedEntity =
                workoutDao.getWorkoutById(workoutId) ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Workout")
                    .withContext("id", workoutId)

            // Try to sync with server if online
            if (networkManager.isOnline()) {
                try {
                    val request = PatchWorkoutRequest(name = newName)

                    val response = workoutsApi.updateWorkout(
                        UUID.fromString(updatedEntity.trainingProgramId),
                        UUID.fromString(workoutId),
                        request
                    )

                    if (response.isSuccessful) {
                        // Update local sync status
                        workoutDao.updateSyncStatus(workoutId, SyncStatus.SYNCED)

                        // Refresh workout list
                        refreshTrigger.emit(updatedEntity.trainingProgramId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing workout update")
                    // Continue with local update
                }
            }

            // Get workout exercises
            val exercises = mutableListOf<WorkoutExercise>()
            workoutExerciseDao.getAllByWorkoutId(workoutId)
                .catch { e ->
                    Timber.e(e, "Error loading exercises for workout $workoutId")
                }
                .collect { entities ->
                    entities.forEach { entity ->
                        val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                        if (exercise != null) {
                            exercises.add(entity.toDomain(exercise))
                        }
                    }
                }

            // Return updated domain model
            updatedEntity.toDomain(exercises)
        }

    override suspend fun deleteWorkout(workoutId: String): Result<Boolean> = runDomainCatching {
        // Get workout entity
        val workoutEntity =
            workoutDao.getWorkoutById(workoutId) ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "Workout")
                .withContext("id", workoutId)

        // Delete locally - we directly delete instead of marking for deletion
        workoutDao.delete(workoutId)
        Timber.d("Deleted local workout: ${workoutEntity.id}")

        // Try to sync with server if online
        if (networkManager.isOnline()) {
            try {
                val response = workoutsApi.deleteWorkout(
                    UUID.fromString(workoutEntity.trainingProgramId),
                    UUID.fromString(workoutId)
                )

                if (!response.isSuccessful) {
                    Timber.w("Failed to delete workout on server: ${response.code()}")
                }

                // Refresh workout list
                refreshTrigger.emit(workoutEntity.trainingProgramId)
            } catch (e: Exception) {
                Timber.e(e, "Error syncing workout deletion")
                // Continue with local deletion
            }
        }

        true
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        try {
            // Get workout entity
            val workoutEntity = workoutDao.getWorkoutById(workoutId) ?: return null

            // Get workout exercises
            val exercises = mutableListOf<WorkoutExercise>()
            workoutExerciseDao.getAllByWorkoutId(workoutId)
                .catch { e ->
                    Timber.e(e, "Error loading exercises for workout $workoutId")
                }
                .collect { entities ->
                    entities.forEach { entity ->
                        val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                        if (exercise != null) {
                            exercises.add(entity.toDomain(exercise))
                        }
                    }
                }

            // Return domain model
            return workoutEntity.toDomain(exercises)
        } catch (e: Exception) {
            Timber.e(e, "Error getting workout: $workoutId")
            return null
        }
    }

    override suspend fun reorderWorkout(workoutId: String, newPosition: Int): Result<Boolean> =
        runDomainCatching {
            // Validate position
            if (newPosition < 0) {
                throw DomainError.ValidationError.InvalidName(
                    "Position",
                    "Position cannot be negative"
                )
            }

            // Get workout entity
            val workoutEntity =
                workoutDao.getWorkoutById(workoutId) ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Workout")
                    .withContext("id", workoutId)

            // Update position locally
            workoutDao.reorderWorkouts(workoutId, newPosition)
            Timber.d("Reordered workout ${workoutEntity.id} to position $newPosition")

            // Try to sync with server if online
            if (networkManager.isOnline()) {
                try {
                    val request = ReorderWorkoutRequest(position = newPosition)

                    val response = workoutsApi.reorderWorkout(
                        UUID.fromString(workoutEntity.trainingProgramId),
                        UUID.fromString(workoutId),
                        request
                    )

                    if (!response.isSuccessful) {
                        Timber.w("Failed to reorder workout on server: ${response.code()}")
                    }

                    // Refresh workout list
                    refreshTrigger.emit(workoutEntity.trainingProgramId)
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing workout reordering")
                    // Continue with local reordering
                }
            }

            true
        }

    /**
     * Helper method to get comparator for sorting workouts
     */
    private fun getSortComparator(sortCriteria: WorkoutRepository.SortCriteria): Comparator<Workout> {
        return when (sortCriteria) {
            WorkoutRepository.SortCriteria.POSITION -> compareBy { it.position }
            WorkoutRepository.SortCriteria.NAME -> compareBy { it.name }
            WorkoutRepository.SortCriteria.EXERCISE_COUNT -> compareByDescending { it.exercises.size }
        }
    }

    /**
     * Validate workout before saving
     */
    private fun validateWorkout(workout: Workout) {
        if (workout.name.isBlank()) {
            throw DomainError.ValidationError.InvalidName(
                workout.name,
                "Workout name cannot be empty"
            )
        }

        if (workout.name.length > Workout.MAX_NAME_LENGTH) {
            throw DomainError.ValidationError.InvalidName(
                workout.name,
                "Workout name cannot exceed ${Workout.MAX_NAME_LENGTH} characters"
            )
        }
    }
}