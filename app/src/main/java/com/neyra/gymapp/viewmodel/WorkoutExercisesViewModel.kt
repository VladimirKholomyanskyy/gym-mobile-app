package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.openapi.apis.ExercisesApi
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import com.neyra.gymapp.openapi.apis.WorkoutSessionsApi
import com.neyra.gymapp.openapi.models.Exercise
import com.neyra.gymapp.openapi.models.StartWorkoutSessionRequest
import com.neyra.gymapp.openapi.models.WorkoutExerciseRequest
import com.neyra.gymapp.openapi.models.WorkoutExerciseResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WorkoutExercisesViewModel @Inject constructor(
    private val workoutExercisesApi: WorkoutExercisesApi,
    private val exercisesApi: ExercisesApi,
    private val workoutSessionsApi: WorkoutSessionsApi
) : ViewModel() {
    // State for workout exercises
    private val _workoutExercises = MutableStateFlow<List<WorkoutExerciseResponse>>(emptyList())
    val workoutExercises: StateFlow<List<WorkoutExerciseResponse>> = _workoutExercises

    // State for exercise ID to Exercise mapping
    private val _exercisesMap = MutableStateFlow<Map<String, Exercise>>(emptyMap())
    val exercisesMap: StateFlow<Map<String, Exercise>> = _exercisesMap

    // State for all exercises
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    // Error message state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCreateWorkoutExerciseDrawerVisible = MutableStateFlow(false)
    val isCreateWorkoutExerciseDrawerVisible: StateFlow<Boolean> =
        _isCreateWorkoutExerciseDrawerVisible

    private val _isUpdateWorkoutExerciseDrawerVisible = MutableStateFlow(false)
    val isUpdateWorkoutExerciseDrawerVisible: StateFlow<Boolean> =
        _isUpdateWorkoutExerciseDrawerVisible

    private val _selectedWorkoutExercise = MutableStateFlow<WorkoutExerciseResponse?>(null)
    val selectedWorkoutExercise: StateFlow<WorkoutExerciseResponse?> = _selectedWorkoutExercise

    init {
        fetchExercises()
    }

    // Fetch workout exercises and corresponding exercise details
    fun fetch(workoutId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val workoutExercises = fetchWorkoutExercises(workoutId)
                val exercisesMap = fetchExercisesMap(workoutExercises)
                _workoutExercises.value = workoutExercises
                _exercisesMap.value = exercisesMap
            } catch (e: Exception) {
                handleError(e, "Failed to fetch workout exercises")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addExerciseToWorkout(
        workoutId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val request = WorkoutExerciseRequest(workoutId, exerciseId, sets, reps)
                val response: Response<WorkoutExerciseResponse> =
                    workoutExercisesApi.postWorkoutExercise(request)
                if (response.isSuccessful) {
                    response.body()?.let { newWorkoutExercise ->
                        // Add the newly created program to the list
                        _workoutExercises.value += newWorkoutExercise
                        _isCreateWorkoutExerciseDrawerVisible.value = false
                    }

                } else {
                    //handle
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startWorkoutSession(workoutId: String, onStartWorkoutSession: (sessionId: String) -> Unit) {
        viewModelScope.launch {

            try {
                val request = StartWorkoutSessionRequest(workoutId)
                val response = workoutSessionsApi.addWorkoutSession(request)
                if (response.isSuccessful) {
                    response.body()?.let { workoutSession ->
                        onStartWorkoutSession(workoutSession.id) // Navigate after success
                    }
                } else {
                    //handle
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateExerciseInWorkout(
        workoutId: String,
        workoutExerciseId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val request = WorkoutExerciseRequest(workoutId, exerciseId, sets, reps)
                val response: Response<WorkoutExerciseResponse> =
                    workoutExercisesApi.patchWorkoutExercise(workoutExerciseId, request)
                if (response.isSuccessful) {
                    response.body()?.let { updatedWorkoutExercise ->
                        // Add the newly created program to the list
                        _workoutExercises.value =
                            _workoutExercises.value.filter { it.id != workoutExerciseId }
                        _workoutExercises.value += updatedWorkoutExercise
                        _isUpdateWorkoutExerciseDrawerVisible.value = false
                    }

                } else {
                    //handle
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSelectedWorkoutExercise(workout: WorkoutExerciseResponse) {
        _selectedWorkoutExercise.value = workout
    }

    fun removeExerciseFromWorkout(workoutExerciseId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = workoutExercisesApi.deleteWorkoutExercise(workoutExerciseId)
                if (response.isSuccessful) {
                    _workoutExercises.value =
                        _workoutExercises.value.filter { it.id != workoutExerciseId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }

        }
    }

    fun showCreateWorkoutExerciseDrawer() {
        _isCreateWorkoutExerciseDrawerVisible.value = true
    }

    fun hideCreateWorkoutExerciseDrawer() {
        _isCreateWorkoutExerciseDrawerVisible.value = false
    }

    fun showUpdateWorkoutExerciseDrawer() {
        _isUpdateWorkoutExerciseDrawerVisible.value = true
    }

    fun hideUpdateWorkoutExerciseDrawer() {
        _isUpdateWorkoutExerciseDrawerVisible.value = false
    }

    // Fetch the list of all exercises
    private fun fetchExercises() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = exercisesApi.listExercises()
                if (response.isSuccessful) {
                    _exercises.value = response.body() ?: emptyList()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Error fetching exercises: ${response.code()}"
                }
            } catch (e: Exception) {
                handleError(e, "Failed to fetch exercises")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch workout exercises from the API
    private suspend fun fetchWorkoutExercises(workoutId: String): List<WorkoutExerciseResponse> {
        val response = workoutExercisesApi.listWorkoutExercises(workoutId)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            _errorMessage.value = "Error fetching workout exercises: ${response.code()}"
            emptyList()
        }
    }

    // Fetch exercises by their IDs in parallel
    private suspend fun fetchExercisesMap(
        workoutExercises: List<WorkoutExerciseResponse>
    ): Map<String, Exercise> = coroutineScope {
        workoutExercises.map { workoutExercise ->
            async {
                val response = exercisesApi.getExerciseById(workoutExercise.exerciseId)
                if (response.isSuccessful) {
                    response.body()?.let { workoutExercise.exerciseId to it }
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull().toMap()
    }

    // Handle exceptions with optional custom messages
    private fun handleError(exception: Exception, message: String? = null) {
        exception.printStackTrace()
        _errorMessage.value = message ?: "An error occurred: ${exception.message}"
    }
}


