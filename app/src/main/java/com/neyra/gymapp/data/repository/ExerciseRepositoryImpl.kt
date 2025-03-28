package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.error.runDomainCatching
import com.neyra.gymapp.domain.model.Exercise
import com.neyra.gymapp.domain.model.toDomain
import com.neyra.gymapp.domain.repository.ExerciseRepository
import com.neyra.gymapp.openapi.apis.ExercisesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exercisesApi: ExercisesApi,
    private val networkManager: NetworkManager
) : ExerciseRepository {

    // Shared flow for triggering exercise refreshes
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        // Initialize with a trigger to load data on first collection
        refreshTrigger.tryEmit(Unit)
    }

    override suspend fun getExerciseById(exerciseId: String): Exercise? {
        try {
            // First try to get from local database
            val localExercise = exerciseDao.getExerciseById(exerciseId)
            if (localExercise != null) {
                return localExercise.toDomain()
            }

            // If not found locally and online, try to fetch from API
            if (networkManager.isOnline()) {
                try {
                    val response = exercisesApi.getExerciseById(UUID.fromString(exerciseId))
                    if (response.isSuccessful && response.body() != null) {
                        val exercise = response.body()!!

                        // Convert the API model to an entity and save to the local database
                        val exerciseEntity = com.neyra.gymapp.data.entities.ExerciseEntity(
                            id = exercise.id.toString(),
                            name = exercise.name,
                            primaryMuscle = exercise.primaryMuscle ?: "",
                            secondaryMuscles = exercise.secondaryMuscle ?: emptyList(),
                            equipment = exercise.equipment ?: "",
                            description = exercise.description ?: ""
                        )
                        exerciseDao.insertExercise(exerciseEntity)

                        return exerciseEntity.toDomain()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error fetching exercise from API: $exerciseId")
                }
            }

            return null
        } catch (e: Exception) {
            Timber.e(e, "Error getting exercise: $exerciseId")
            return null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getExercises(
        filter: ExerciseRepository.FilterCriteria?,
        sortBy: ExerciseRepository.SortCriteria
    ): Flow<List<Exercise>> {
        // Emit initial value to trigger first load
        refreshTrigger.tryEmit(Unit)

        // Use flatMapLatest to efficiently handle refreshes
        return refreshTrigger.flatMapLatest {
            // First emit from the local database
            flow {
                val exercises = exerciseDao.getAllExercises()
                emit(exercises.map { it.toDomain() })
            }.catch { e ->
                Timber.e(e, "Error loading exercises from database")
                emit(emptyList())
            }
        }.map { exercises ->
            // Apply filtering if filter criteria is provided
            val filtered = if (filter != null) {
                exercises.filter { exercise ->
                    (filter.muscleGroup == null || exercise.primaryMuscle == filter.muscleGroup) &&
                            (filter.equipment == null || exercise.equipment == filter.equipment) &&
                            (filter.difficulty == null)  // Difficulty filtering could be added if available
                }
            } else {
                exercises
            }

            // Apply sorting based on criteria
            when (sortBy) {
                ExerciseRepository.SortCriteria.NAME -> filtered.sortedBy { it.name }
                ExerciseRepository.SortCriteria.MUSCLE_GROUP -> filtered.sortedBy { it.primaryMuscle }
                ExerciseRepository.SortCriteria.EQUIPMENT -> filtered.sortedBy { it.equipment }
            }
        }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> {
        // If query is empty, return all exercises
        if (query.isBlank()) {
            return getExercises()
        }

        // Otherwise, search by name
        return flow {
            val exercises = exerciseDao.searchExercisesByName("%${query.trim()}%")
            emit(exercises.map { it.toDomain() })
        }.catch { e ->
            Timber.e(e, "Error searching exercises: $query")
            emit(emptyList())
        }
    }

    override suspend fun refreshExercises(): Result<Boolean> = runDomainCatching {
        if (!networkManager.isOnline()) {
            throw DomainError.NetworkError.NoConnection()
                .withContext("operation", "refreshExercises")
        }

        try {
            // Fetch exercises from API
            val response = exercisesApi.listExercises(page = 1, pageSize = 100)
            if (response.isSuccessful && response.body() != null) {
                val exercises = response.body()!!.items ?: emptyList()

                // Convert to entities and save to database
                val entities = exercises.map { exercise ->
                    com.neyra.gymapp.data.entities.ExerciseEntity(
                        id = exercise.id.toString(),
                        name = exercise.name,
                        primaryMuscle = exercise.primaryMuscle ?: "",
                        secondaryMuscles = exercise.secondaryMuscle ?: emptyList(),
                        equipment = exercise.equipment ?: "",
                        description = exercise.description ?: ""
                    )
                }

                exerciseDao.insertAllExercises(entities)

                // Trigger a refresh of the flow
                refreshTrigger.emit(Unit)

                return@runDomainCatching true
            } else {
                throw DomainError.NetworkError.ServerError(
                    response.code(),
                    "Failed to fetch exercises: ${response.errorBody()?.string()}"
                )
            }
        } catch (e: Exception) {
            if (e is DomainError) throw e

            Timber.e(e, "Error refreshing exercises")
            throw DomainError.NetworkError.ServerError(
                500,
                "Failed to refresh exercises: ${e.message}"
            )
        }
    }
}