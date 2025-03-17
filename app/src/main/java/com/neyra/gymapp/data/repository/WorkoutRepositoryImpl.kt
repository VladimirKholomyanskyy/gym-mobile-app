package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.mapper.toDomain
import com.neyra.gymapp.domain.mapper.toDomainExerciseList
import com.neyra.gymapp.domain.mapper.toEntity
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.repository.WorkoutRepository
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import com.neyra.gymapp.openapi.models.CreateWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.CreateWorkoutRequest
import com.neyra.gymapp.openapi.models.PatchWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.PatchWorkoutRequest
import com.neyra.gymapp.openapi.models.ReorderWorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.ReorderWorkoutRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutsApi: WorkoutsApi,
    private val workoutExercisesApi: WorkoutExercisesApi,
    private val networkManager: NetworkManager
) : WorkoutRepository {

    override suspend fun createWorkout(workout: Workout): Result<Workout> {
        return try {
            // Convert to entity and save locally
            val workoutEntity = workout.toEntity()
            workoutDao.insert(workoutEntity)

            // If online, sync with server
            if (networkManager.isOnline()) {
                val request = CreateWorkoutRequest(name = workout.name)
                val response = workoutsApi.addWorkoutToProgram(
                    UUID.fromString(workout.trainingProgramId),
                    request
                )

                if (response.isSuccessful) {
                    // Update local entity with server response if needed
                    response.body()?.let { serverWorkout ->
                        val updatedEntity = workoutEntity.copy(id = serverWorkout.id.toString())
                        workoutDao.insert(updatedEntity)

                        // Return updated domain model
                        return Result.success(updatedEntity.toDomain())
                    }
                }
            }

            // Return the locally created workout
            Result.success(workoutEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkout(workoutId: String, workout: Workout): Result<Workout> {
        return try {
            // Update locally
            workoutDao.update(workoutId, workout.name)

            // If online, sync with server
            if (networkManager.isOnline()) {
                val request = PatchWorkoutRequest(name = workout.name)
                workoutsApi.updateWorkout(
                    UUID.fromString(workout.trainingProgramId),
                    UUID.fromString(workoutId),
                    request
                )
            }

            // Return updated workout
            val updatedWorkout = workoutDao.getWorkoutById(workoutId)?.toDomain()
                ?: return Result.failure(IllegalStateException("Failed to retrieve updated workout"))

            Result.success(updatedWorkout)
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

                    WorkoutRepository.SortCriteria.CREATED_DATE ->
                        workoutsWithExercises.sortedByDescending { it.createdAt }
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

    override suspend fun syncWorkouts(trainingProgramId: String): Result<Int> {
        if (!networkManager.isOnline()) {
            return Result.failure(IllegalStateException("No network connection available"))
        }

        return try {
            // Fetch workouts from server
            val response = workoutsApi.listWorkoutsForProgram(UUID.fromString(trainingProgramId))

            if (response.isSuccessful) {
                val remoteWorkouts = response.body()?.items ?: emptyList()
                var syncCount = 0

                // Process each remote workout
                remoteWorkouts.forEach { remoteWorkout ->
                    // Insert or update in local database
                    val localWorkout = workoutDao.getWorkoutById(remoteWorkout.id.toString())

                    if (localWorkout == null) {
                        // New workout - insert locally
                        workoutDao.insert(
                            com.neyra.gymapp.data.entities.WorkoutEntity(
                                id = remoteWorkout.id.toString(),
                                name = remoteWorkout.name,
                                trainingProgramId = trainingProgramId,
                                position = remoteWorkout.position.toInt()
                            )
                        )
                    } else if (localWorkout.name != remoteWorkout.name ||
                        localWorkout.position != remoteWorkout.position.toInt()
                    ) {
                        // Update existing workout if there are changes
                        workoutDao.insert(
                            localWorkout.copy(
                                name = remoteWorkout.name,
                                position = remoteWorkout.position.toInt()
                            )
                        )
                    }

                    syncCount++

                    // Sync exercises for this workout
                    syncWorkoutExercises(remoteWorkout.id.toString())
                }

                Result.success(syncCount)
            } else {
                Result.failure(IllegalStateException("Failed to fetch workouts from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper method to sync workout exercises for a specific workout
     */
    private suspend fun syncWorkoutExercises(workoutId: String): Result<Int> {
        if (!networkManager.isOnline()) {
            return Result.failure(IllegalStateException("No network connection available"))
        }

        return try {
            // Fetch exercises from server
            val response = workoutExercisesApi.listWorkoutExercises(UUID.fromString(workoutId))

            if (response.isSuccessful) {
                val remoteExercises = response.body()?.items ?: emptyList()
                var syncCount = 0

                // Process each remote exercise
                remoteExercises.forEach { remoteExercise ->
                    // Insert or update in local database
                    val localExercise = workoutExerciseDao.getById(remoteExercise.id.toString())

                    if (localExercise == null) {
                        // New exercise - insert locally
                        workoutExerciseDao.insert(
                            com.neyra.gymapp.data.entities.WorkoutExerciseEntity(
                                id = remoteExercise.id.toString(),
                                workoutId = remoteExercise.workoutId.toString(),
                                exerciseId = remoteExercise.exerciseId.toString(),
                                sets = remoteExercise.sets,
                                reps = remoteExercise.reps,
                                position = remoteExercise.position
                            )
                        )
                    } else if (localExercise.sets != remoteExercise.sets ||
                        localExercise.reps != remoteExercise.reps ||
                        localExercise.position != remoteExercise.position
                    ) {
                        // Update existing exercise if there are changes
                        workoutExerciseDao.insert(
                            localExercise.copy(
                                sets = remoteExercise.sets,
                                reps = remoteExercise.reps,
                                position = remoteExercise.position
                            )
                        )
                    }

                    syncCount++
                }

                Result.success(syncCount)
            } else {
                Result.failure(IllegalStateException("Failed to fetch workout exercises from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}