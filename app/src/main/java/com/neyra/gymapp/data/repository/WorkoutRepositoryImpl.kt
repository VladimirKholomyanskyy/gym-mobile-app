package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.error.runDomainCatching
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toDomainExerciseList
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.repository.WorkoutRepository
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import com.neyra.gymapp.openapi.models.CreateWorkoutRequest
import com.neyra.gymapp.openapi.models.PatchWorkoutRequest
import com.neyra.gymapp.openapi.models.ReorderWorkoutRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        Timber.d("Creating workout: ${workout.name}")
        validateWorkout(workout)
        // Convert to entity and save locally
        val workoutEntity = workout.toEntity()
        workoutDao.insert(workoutEntity)
        Timber.d("Saved training program locally with ID: ${workoutEntity.id}")

        if (networkManager.isOnline()) {
            Timber.d("Network available, syncing program with server")
            try {
                val request = CreateWorkoutRequest(name = workout.name)
                val response = workoutsApi.addWorkoutToProgram(
                    UUID.fromString(workout.trainingProgramId),
                    request
                )

                if (response.isSuccessful && response.body() != null) {
                    val remoteId = response.body()!!.id.toString()
                    Timber.d("Workout synced successfully with remote ID: $remoteId")
                    workoutDao.updateIdAndSyncStatus(
                        workoutEntity.id,
                        remoteId,
                        SyncStatus.SYNCED,
                        response.body()!!.createdAt.toInstant().toEpochMilli(),
                        response.body()!!.updatedAt.toInstant().toEpochMilli()
                    )
                    return@runDomainCatching workout.copy(id = remoteId)
                } else {
                    val statusCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.w("Server responded with error $statusCode: $errorBody")
                    throw DomainError.fromHttpStatus(statusCode, errorBody)
                        .withContext("workoutName", workout.name)
                        .withContext("trainingProgramId", workout.trainingProgramId)
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
        // Return the locally created workout
        workoutEntity.copy(id = workoutEntity.id).toDomain()
    }

    override suspend fun updateWorkout(workoutId: String, newName: String): Result<Workout> {
        return try {
            val rowsUpdated = workoutDao.updateName(
                workoutId, newName,
                SyncStatus.PENDING_UPDATE,
                System.currentTimeMillis()
            )
            if (rowsUpdated > 0) {
                // Fetch and return the updated entity
                val updatedWorkout = workoutDao.getWorkoutById(workoutId) ?: return Result.failure(
                    Exception("Workout not found after update")
                )
                if (networkManager.isOnline()) {
                    val response = workoutsApi.updateWorkout(
                        UUID.fromString(updatedWorkout.trainingProgramId),
                        UUID.fromString(workoutId),
                        PatchWorkoutRequest(name = updatedWorkout.name)
                    )

                    if (response.isSuccessful) {
                        // Mark as synced
                        workoutDao.updateSyncStatus(workoutId, SyncStatus.SYNCED)
                    }

                }
                Result.success(updatedWorkout.toDomain())
            } else {
                Result.failure(Exception("No rows updated. Training program may not exist."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWorkout(workoutId: String): Result<Boolean> {
        return try {
            // Get workout details before deletion
            val workout = workoutDao.getWorkoutById(workoutId)
                ?: return Result.failure(IllegalArgumentException("Workout not found"))

            // Delete locally
            workoutDao.delete(workoutId)

            // If online, sync with server
            if (networkManager.isOnline()) {
                workoutsApi.deleteWorkout(
                    UUID.fromString(workout.trainingProgramId),
                    UUID.fromString(workoutId)
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        // Get the workout entity
        val workoutEntity = workoutDao.getWorkoutById(workoutId) ?: return null

        // Get the exercises for this workout - collect first emission
        val exerciseList = workoutExerciseDao.getAllByWorkoutId(workoutId)
            .map { entities ->
                // Create a map of exercise IDs to their info (name, primary muscle)
                val exerciseInfoMap = entities.associate { entity ->
                    val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                    entity.exerciseId to Pair(
                        exercise?.name ?: "",
                        exercise?.primaryMuscle ?: ""
                    )
                }

                // Convert entities to domain models with exercise info
                entities.toDomainExerciseList(exerciseInfoMap)
            }
            .first() // Collect the first emission

        // Return the domain model with exercises
        return workoutEntity.toDomain(exerciseList)
    }

    override fun getWorkouts(
        trainingProgramId: String,
        sortBy: WorkoutRepository.SortCriteria
    ): Flow<List<Workout>> {
        // Get workout entities
        return workoutDao.getWorkoutsByTrainingProgramId(trainingProgramId)
            .map { workouts ->
                // Create a list of workout IDs
                val workoutIds = workouts.map { it.id }

                // For each workout, get its exercises
                val workoutsWithExercises = workouts.map { workout ->
                    // Get exercises for this workout
                    val exercises =
                        workoutExerciseDao.getAllByWorkoutId(workout.id).map { entities ->
                            // Create a map of exercise IDs to their info
                            val exerciseInfoMap = entities.associate { entity ->
                                val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                                entity.exerciseId to Pair(
                                    exercise?.name ?: "",
                                    exercise?.primaryMuscle ?: ""
                                )
                            }

                            // Convert entities to domain models
                            entities.toDomainExerciseList(exerciseInfoMap)
                        }

                    // Convert workout entity to domain model
                    workout.toDomain(exercises.firstOrNull() ?: emptyList())
                }

                // Apply sorting based on criteria
                when (sortBy) {
                    WorkoutRepository.SortCriteria.POSITION ->
                        workoutsWithExercises.sortedBy { it.position }

                    WorkoutRepository.SortCriteria.NAME ->
                        workoutsWithExercises.sortedBy { it.name }

                    WorkoutRepository.SortCriteria.EXERCISE_COUNT ->
                        workoutsWithExercises.sortedByDescending { it.exercises.size }
                }
            }
    }

    override suspend fun reorderWorkout(workoutId: String, newPosition: Int): Result<Boolean> {
        return try {
            // Get workout details
            val workout = workoutDao.getWorkoutById(workoutId)
                ?: return Result.failure(IllegalArgumentException("Workout not found"))

            // Update position locally
            workoutDao.updatePosition(workoutId, newPosition)

            // If online, sync with server
            if (networkManager.isOnline()) {
                val request = ReorderWorkoutRequest(position = newPosition)
                workoutsApi.reorderWorkout(
                    UUID.fromString(workout.trainingProgramId),
                    UUID.fromString(workoutId),
                    request
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates a workout before saving it.
     * Throws appropriate domain errors if validation fails.
     */
    private fun validateWorkout(workout: Workout) {
        // Name validation
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