package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toDomainExerciseList
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.repository.WorkoutExerciseRepository
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import com.neyra.gymapp.openapi.models.CreateWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.PatchWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.ReorderWorkoutExerciseRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutExerciseRepositoryImpl @Inject constructor(
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val workoutExerciseApi: WorkoutExercisesApi,
    private val networkManager: NetworkManager
) : WorkoutExerciseRepository {

    override suspend fun addExerciseToWorkout(workoutExercise: WorkoutExercise): Result<WorkoutExercise> {
        return try {
            // Convert to entity and save locally
            val entity = workoutExercise.toEntity()
            workoutExerciseDao.insert(entity)

            // If online, sync with server
            if (networkManager.isOnline()) {
                val request = CreateWorkoutExerciseRequest(
                    workoutId = UUID.fromString(workoutExercise.workoutId),
                    exerciseId = UUID.fromString(workoutExercise.exerciseId),
                    sets = workoutExercise.sets,
                    reps = workoutExercise.reps
                )
                val response = workoutExercisesApi.addWorkoutExercise(request)

                if (response.isSuccessful) {
                    // Update local entity with server response
                    response.body()?.let { serverExercise ->
                        val updatedEntity = entity.copy(id = serverExercise.id.toString())
                        workoutExerciseDao.insert(updatedEntity)

                        // Get exercise details
                        val exercise = exerciseDao.getExerciseById(workoutExercise.exerciseId)

                        // Return updated domain model
                        return Result.success(
                            updatedEntity.toDomain(
                                exerciseName = exercise?.name ?: "",
                                primaryMuscle = exercise?.primaryMuscle ?: ""
                            )
                        )
                    }
                }
            }

            // Get exercise details
            val exercise = exerciseDao.getExerciseById(workoutExercise.exerciseId)

            // Return the locally created workout exercise
            Result.success(
                entity.toDomain(
                    exerciseName = exercise?.name ?: "",
                    primaryMuscle = exercise?.primaryMuscle ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkoutExercise(
        workoutExerciseId: String,
        workoutExercise: WorkoutExercise
    ): Result<WorkoutExercise> {
        return try {
            // Get the current entity
            val currentEntity = workoutExerciseDao.getById(workoutExerciseId)
                ?: return Result.failure(IllegalArgumentException("Workout exercise not found"))

            // Update locally
            val updatedEntity = currentEntity.copy(
                sets = workoutExercise.sets,
                reps = workoutExercise.reps
            )
            workoutExerciseDao.insert(updatedEntity)

            // If online, sync with server
            if (networkManager.isOnline()) {
                val request = PatchWorkoutExerciseRequest(
                    sets = updatedEntity.sets,
                    reps = updatedEntity.reps
                )
                workoutExercisesApi.updateWorkoutExercise(
                    UUID.fromString(workoutExerciseId),
                    request
                )
            }

            // Get exercise details
            val exercise = exerciseDao.getExerciseById(updatedEntity.exerciseId)

            // Return updated domain model
            Result.success(
                updatedEntity.toDomain(
                    exerciseName = exercise?.name ?: "",
                    primaryMuscle = exercise?.primaryMuscle ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWorkoutExercise(workoutExerciseId: String): Result<Boolean> {
        return try {
            // Get current entity before deletion
            val entity = workoutExerciseDao.getById(workoutExerciseId)
                ?: return Result.failure(IllegalArgumentException("Workout exercise not found"))

            // Delete locally
            workoutExerciseDao.deleteById(workoutExerciseId)

            // If online, sync with server
            if (networkManager.isOnline()) {
                workoutExercisesApi.deleteWorkoutExercise(UUID.fromString(workoutExerciseId))
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getWorkoutExercises(workoutId: String): Flow<List<WorkoutExercise>> {
        return workoutExerciseDao.getAllByWorkoutId(workoutId).map { entities ->
            // Create a map of exercise IDs to their info (name, primary muscle)
            val exerciseInfoMap = entities.associate { entity ->
                val exercise = exerciseDao.getExerciseById(entity.exerciseId)
                entity.exerciseId to Pair(exercise?.name ?: "", exercise?.primaryMuscle ?: "")
            }

            // Convert entities to domain models with exercise info
            entities.toDomainExerciseList(exerciseInfoMap)
        }
    }

    override suspend fun reorderWorkoutExercise(
        workoutExerciseId: String,
        newPosition: Int
    ): Result<Boolean> {
        return try {
            // Update position locally
            workoutExerciseDao.updatePosition(workoutExerciseId, newPosition)

            // If online, sync with server
            if (networkManager.isOnline()) {
                val request = ReorderWorkoutExerciseRequest(position = newPosition)
                workoutExercisesApi.reorderWorkoutExercise(
                    UUID.fromString(workoutExerciseId),
                    request
                )
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}