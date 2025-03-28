package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutsApi: WorkoutsApi,
    private val networkManager: NetworkManager
) : WorkoutRepository {

    override suspend fun createWorkout(workout: Workout): Result<Workout> = runDomainCatching {
        Timber.d("Creating workout: ${workout.name} for program: ${workout.trainingProgramId}")
        validateWorkout(workout)

        // Convert to entity and save locally
        val workoutEntity = workout.toEntity()
        workoutDao.insert(workoutEntity)

        // Try to sync with server if online
        if (networkManager.isOnline()) {
            try {
                val request = CreateWorkoutRequest(name = workout.name)
                val response = workoutsApi.addWorkoutToProgram(
                    UUID.fromString(workout.trainingProgramId),
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val remoteId = response.body()!!.id.toString()
                    val createdAt = response.body()!!.createdAt.toInstant().toEpochMilli()
                    val updatedAt = response.body()!!.updatedAt.toInstant().toEpochMilli()

                    workoutDao.updateIdAndSyncStatus(
                        workoutEntity.id,
                        remoteId,
                        SyncStatus.SYNCED,
                        createdAt,
                        updatedAt
                    )

                    return@runDomainCatching workout.copy(id = remoteId)
                } else {
                    val statusCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    throw DomainError.fromHttpStatus(statusCode, errorBody)
                        .withContext("workoutName", workout.name)
                        .withContext("trainingProgramId", workout.trainingProgramId)
                }
            } catch (e: Exception) {
                if (e is DomainError) throw e

                Timber.e(e, "Network error during workout creation")
                // Continue with local entity if network sync failed
            }
        }

        // Return the locally created workout
        workout.copy(id = workoutEntity.id)
    }

    override suspend fun updateWorkout(workoutId: String, newName: String): Result<Workout> =
        runDomainCatching {
            if (newName.isBlank() || newName.length > Workout.MAX_NAME_LENGTH) {
                throw DomainError.ValidationError.InvalidName(
                    newName,
                    "Workout name must be between 1 and ${Workout.MAX_NAME_LENGTH} characters"
                )
            }

            val rowsUpdated = workoutDao.updateName(
                workoutId,
                newName,
                SyncStatus.PENDING_UPDATE,
                System.currentTimeMillis()
            )

            if (rowsUpdated <= 0) {
                throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Workout")
                    .withContext("id", workoutId)
            }

            // Fetch updated entity
            val updatedWorkout = workoutDao.getWorkoutById(workoutId)
                ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Workout")
                    .withContext("id", workoutId)

            // Try to sync with server if online
            if (networkManager.isOnline()) {
                try {
                    val response = workoutsApi.updateWorkout(
                        UUID.fromString(updatedWorkout.trainingProgramId),
                        UUID.fromString(workoutId),
                        PatchWorkoutRequest(name = updatedWorkout.name)
                    )

                    if (response.isSuccessful) {
                        workoutDao.updateSyncStatus(workoutId, SyncStatus.SYNCED)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync workout update")
                    // Continue with local update
                }
            }

            updatedWorkout.toDomain()
        }

    override suspend fun deleteWorkout(workoutId: String): Result<Boolean> = runDomainCatching {
        // Get workout details before deletion
        val workout = workoutDao.getWorkoutById(workoutId)
            ?: throw DomainError.DataError.NotFound()
                .withContext("entityType", "Workout")
                .withContext("id", workoutId)

        // Delete locally
        workoutDao.delete(workoutId)

        // If online, sync with server
        if (networkManager.isOnline()) {
            try {
                workoutsApi.deleteWorkout(
                    UUID.fromString(workout.trainingProgramId),
                    UUID.fromString(workoutId)
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync workout deletion")
                // Continue with local deletion
            }
        }

        true
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        // Get the workout entity
        val workoutEntity = workoutDao.getWorkoutById(workoutId) ?: return null

        // Get exercises for this workout
        val exercises = workoutExerciseDao.getAllByWorkoutId(workoutId)
            .map { mapExerciseEntitiesToDomain(it) }
            .firstOrNull() ?: emptyList()

        // Return the domain model with exercises
        return workoutEntity.toDomain(exercises)
    }

    override fun getWorkouts(
        trainingProgramId: String,
        sortBy: WorkoutRepository.SortCriteria
    ): Flow<List<Workout>> {
        // Get workouts and their exercises
        val workoutsFlow = workoutDao.getWorkoutsByTrainingProgramId(trainingProgramId)

        return workoutsFlow
            .map { workouts ->
                workouts.map { workout ->
                    // For each workout, create a workout domain model with its exercises
                    val exercisesFlow = workoutExerciseDao.getAllByWorkoutId(workout.id)
                        .map { mapExerciseEntitiesToDomain(it) }
                        .catch { emit(emptyList()) }

                    // Return a Pair of the workout entity and its exercises flow
                    workout to exercisesFlow
                }
            }
            .map { workoutPairs ->
                // For each workout pair, get the exercises and create the domain model
                workoutPairs.map { (workoutEntity, exercisesFlow) ->
                    val exercises = exercisesFlow.firstOrNull() ?: emptyList()
                    workoutEntity.toDomain(exercises)
                }
            }
            .map { workouts ->
                // Apply sorting based on criteria
                when (sortBy) {
                    WorkoutRepository.SortCriteria.POSITION -> workouts.sortedBy { it.position }
                    WorkoutRepository.SortCriteria.NAME -> workouts.sortedBy { it.name }
                    WorkoutRepository.SortCriteria.EXERCISE_COUNT -> workouts.sortedByDescending { it.exercises.size }
                }
            }
            .catch { e ->
                Timber.e(e, "Error loading workouts")
                emit(emptyList())
            }
    }

    override suspend fun reorderWorkout(workoutId: String, newPosition: Int): Result<Boolean> =
        runDomainCatching {
            if (newPosition < 0) {
                throw DomainError.ValidationError.InvalidName(
                    "Position",
                    "Position cannot be negative"
                )
            }

            // Get workout details
            val workout = workoutDao.getWorkoutById(workoutId)
                ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Workout")
                    .withContext("id", workoutId)

            // Update position locally
            workoutDao.updatePosition(workoutId, newPosition)

            // If online, sync with server
            if (networkManager.isOnline()) {
                try {
                    val request = ReorderWorkoutRequest(position = newPosition)
                    workoutsApi.reorderWorkout(
                        UUID.fromString(workout.trainingProgramId),
                        UUID.fromString(workoutId),
                        request
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync workout reordering")
                    // Continue with local update
                }
            }

            true
        }

    /**
     * Maps workout exercise entities to domain models with exercise information
     */
    private suspend fun mapExerciseEntitiesToDomain(entities: List<WorkoutExerciseEntity>): List<WorkoutExercise> {
        // Create a map of exercise IDs to their info (name, primary muscle)
        return entities.map { entity ->
            val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                ?: throw DomainError.DataError.NotFound()
                    .withContext("entityType", "Exercise")
                    .withContext("id", entity.exerciseId)
            entity.toDomain(exercise)
        }
    }

    /**
     * Validates a workout before saving it.
     * Throws appropriate domain errors if validation fails.
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

        if (workout.position < 0) {
            throw DomainError.ValidationError.InvalidName(
                "Position",
                "Position cannot be negative"
            )
        }
    }
}