package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.usecase.AddExerciseToWorkoutUseCase
import com.neyra.gymapp.domain.usecase.DeleteWorkoutExerciseUseCase
import com.neyra.gymapp.domain.usecase.GetWorkoutExercisesUseCase
import com.neyra.gymapp.domain.usecase.GetWorkoutsUseCase
import com.neyra.gymapp.domain.usecase.ReorderWorkoutExerciseUseCase
import com.neyra.gymapp.domain.usecase.UpdateWorkoutExerciseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutExercisesViewModel @Inject constructor(
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val getWorkoutExercisesUseCase: GetWorkoutExercisesUseCase,
    private val addExerciseToWorkoutUseCase: AddExerciseToWorkoutUseCase,
    private val updateWorkoutExerciseUseCase: UpdateWorkoutExerciseUseCase,
    private val deleteWorkoutExerciseUseCase: DeleteWorkoutExerciseUseCase,
    private val reorderWorkoutExerciseUseCase: ReorderWorkoutExerciseUseCase
) : ViewModel() {

    // Current workout
    private val _workout = MutableStateFlow<UiState<Workout>>(UiState.Loading)
    val workout: StateFlow<UiState<Workout>> = _workout.asStateFlow()

    // Workout exercises
    private val _workoutExercises =
        MutableStateFlow<UiState<List<WorkoutExercise>>>(UiState.Loading)
    val workoutExercises: StateFlow<UiState<List<WorkoutExercise>>> =
        _workoutExercises.asStateFlow()

    // Exercise mapping for UI (id -> Exercise)
    private val _exercisesMap =
        MutableStateFlow<Map<String, com.neyra.gymapp.openapi.models.Exercise>>(emptyMap())
    val exercisesMap: StateFlow<Map<String, com.neyra.gymapp.openapi.models.Exercise>> =
        _exercisesMap.asStateFlow()

    // Selected workout exercise for edit/delete operations
    private val _selectedWorkoutExercise = MutableStateFlow<WorkoutExercise?>(null)
    val selectedWorkoutExercise: StateFlow<WorkoutExercise?> =
        _selectedWorkoutExercise.asStateFlow()

    // UI state flags
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isCreateWorkoutExerciseDrawerVisible = MutableStateFlow(false)
    val isCreateWorkoutExerciseDrawerVisible: StateFlow<Boolean> =
        _isCreateWorkoutExerciseDrawerVisible.asStateFlow()

    private val _isUpdateWorkoutExerciseDrawerVisible = MutableStateFlow(false)
    val isUpdateWorkoutExerciseDrawerVisible: StateFlow<Boolean> =
        _isUpdateWorkoutExerciseDrawerVisible.asStateFlow()

    // Current workout ID
    private var currentWorkoutId: String? = null

    // Fetch workout and its exercises
    fun fetch(workoutId: String) {
        currentWorkoutId = workoutId

        viewModelScope.launch {
            _isLoading.value = true

            // Load workout details
            try {
                val workout = getWorkoutsUseCase.getById(workoutId)
                if (workout != null) {
                    _workout.value = UiState.Success(workout)
                } else {
                    _workout.value = UiState.Error("Workout not found")
                }
            } catch (e: Exception) {
                handleError(e)
            }

            // Load workout exercises
            fetchWorkoutExercises(workoutId)
        }
    }

    // Fetch workout exercises
    private fun fetchWorkoutExercises(workoutId: String) {
        viewModelScope.launch {
            _workoutExercises.value = UiState.Loading

            getWorkoutExercisesUseCase(workoutId)
                .catch { exception ->
                    handleError(exception)
                }
                .collect { exercises ->
                    _workoutExercises.value = UiState.Success(exercises)
                    _isLoading.value = false
                }
        }
    }

    // Add exercise to workout
    fun addExerciseToWorkout(
        workoutId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = addExerciseToWorkoutUseCase(
                    workoutId,
                    exerciseId,
                    sets,
                    reps
                )

                result.onSuccess { newExercise ->
                    _isCreateWorkoutExerciseDrawerVisible.value = false

                    // Update workout exercises list
                    when (val currentExercises = _workoutExercises.value) {
                        is UiState.Success -> {
                            _workoutExercises.value =
                                UiState.Success(currentExercises.data + newExercise)
                        }

                        else -> fetchWorkoutExercises(workoutId)
                    }

                    _errorMessage.value = null
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update workout exercise
    fun updateExerciseInWorkout(
        workoutId: String,
        workoutExerciseId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = updateWorkoutExerciseUseCase(
                    workoutExerciseId,
                    sets,
                    reps
                )

                result.onSuccess { updatedExercise ->
                    _isUpdateWorkoutExerciseDrawerVisible.value = false

                    // Update workout exercises list
                    when (val currentExercises = _workoutExercises.value) {
                        is UiState.Success -> {
                            val updatedList = currentExercises.data.map {
                                if (it.id == updatedExercise.id) updatedExercise else it
                            }
                            _workoutExercises.value = UiState.Success(updatedList)
                        }

                        else -> fetchWorkoutExercises(workoutId)
                    }

                    _errorMessage.value = null
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Remove exercise from workout
    fun removeExerciseFromWorkout(workoutExerciseId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = deleteWorkoutExerciseUseCase(workoutExerciseId)

                result.onSuccess { success ->
                    if (success) {
                        // Update workout exercises list
                        when (val currentExercises = _workoutExercises.value) {
                            is UiState.Success -> {
                                val updatedList = currentExercises.data.filter {
                                    it.id != workoutExerciseId
                                }
                                _workoutExercises.value = UiState.Success(updatedList)
                            }

                            else -> currentWorkoutId?.let { fetchWorkoutExercises(it) }
                        }

                        _errorMessage.value = null
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reorder workout exercise
    fun reorderExercise(workoutExerciseId: String, newPosition: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = reorderWorkoutExerciseUseCase(workoutExerciseId, newPosition)

                result.onSuccess { success ->
                    if (success) {
                        // Refresh the workout exercises list
                        currentWorkoutId?.let { fetchWorkoutExercises(it) }
                        _errorMessage.value = null
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Start a workout session
    fun startWorkoutSession(workoutId: String, onStartWorkoutSession: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // In a real implementation, this would call a use case to start a workout session
                // For now, we'll just simulate it by returning a placeholder session ID
                // Replace this with actual implementation that calls the appropriate use case
                val sessionId = "session-$workoutId-${System.currentTimeMillis()}"
                onStartWorkoutSession(sessionId)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Set selected workout exercise for edit/delete operations
    fun setSelectedWorkoutExercise(exercise: WorkoutExercise) {
        _selectedWorkoutExercise.value = exercise
    }

    // Show/hide create workout exercise drawer
    fun showCreateWorkoutExerciseDrawer() {
        _isCreateWorkoutExerciseDrawerVisible.value = true
    }

    fun hideCreateWorkoutExerciseDrawerVisible() {
        _isCreateWorkoutExerciseDrawerVisible.value = false
    }

    // Show/hide update workout exercise drawer
    fun showUpdateWorkoutExerciseDrawer() {
        _isUpdateWorkoutExerciseDrawerVisible.value = true
    }

    fun hideUpdateWorkoutExerciseDrawer() {
        _isUpdateWorkoutExerciseDrawerVisible.value = false
    }

    // Error handling
    private fun handleError(error: Throwable) {
        _isLoading.value = false
        val errorMessage = when (error) {
            is DomainError.ValidationError ->
                "Validation error: ${error.message}"

            is DomainError.NetworkError.NoConnection ->
                "No internet connection. Please check your network."

            is DomainError.AuthenticationError ->
                "Authentication error: ${error.message}"

            is DomainError.DataError.NotFound ->
                "The requested resource was not found."

            is DomainError ->
                error.message

            else ->
                "An unexpected error occurred: ${error.message}"
        }

        _errorMessage.value = errorMessage

        // Update the appropriate state flow based on the current operation
        if (_workout.value is UiState.Loading) {
            _workout.value = UiState.Error(errorMessage)
        }

        if (_workoutExercises.value is UiState.Loading) {
            _workoutExercises.value = UiState.Error(errorMessage)
        }
    }
}