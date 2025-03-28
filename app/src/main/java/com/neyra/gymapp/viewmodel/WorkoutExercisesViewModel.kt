package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.domain.usecase.AddExerciseToWorkoutUseCase
import com.neyra.gymapp.domain.usecase.DeleteWorkoutExerciseUseCase
import com.neyra.gymapp.domain.usecase.GetWorkoutExercisesUseCase
import com.neyra.gymapp.domain.usecase.GetWorkoutsUseCase
import com.neyra.gymapp.domain.usecase.ReorderWorkoutExerciseUseCase
import com.neyra.gymapp.domain.usecase.UpdateWorkoutExerciseUseCase
import com.neyra.gymapp.openapi.models.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Unified UI state for workout exercises screen
 */
data class WorkoutExercisesUiState(
    val workout: Workout? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val selectedExercise: WorkoutExercise? = null,
    val availableExercises: Map<String, Exercise> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCreateExerciseDrawerVisible: Boolean = false,
    val isUpdateExerciseDrawerVisible: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false
)

@HiltViewModel
class WorkoutExercisesViewModel @Inject constructor(
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val getWorkoutExercisesUseCase: GetWorkoutExercisesUseCase,
    private val addExerciseToWorkoutUseCase: AddExerciseToWorkoutUseCase,
    private val updateWorkoutExerciseUseCase: UpdateWorkoutExerciseUseCase,
    private val deleteWorkoutExerciseUseCase: DeleteWorkoutExerciseUseCase,
    private val reorderWorkoutExerciseUseCase: ReorderWorkoutExerciseUseCase
) : ViewModel() {

    // Unified UI state
    private val _uiState = MutableStateFlow(WorkoutExercisesUiState())
    val uiState: StateFlow<WorkoutExercisesUiState> = _uiState.asStateFlow()

    // Current workout ID being displayed
    private var currentWorkoutId: String? = null

    /**
     * Loads workout details and its exercises
     */
    fun fetch(workoutId: String) {
        currentWorkoutId = workoutId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Load workout details
            try {
                val workout = getWorkoutsUseCase.getById(workoutId)
                if (workout != null) {
                    _uiState.update { it.copy(workout = workout) }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Workout not found",
                            isLoading = false
                        )
                    }
                    return@launch
                }
            } catch (e: Exception) {
                handleError(e)
                return@launch
            }

            // Load workout exercises
            fetchWorkoutExercises(workoutId)
        }
    }

    /**
     * Fetches exercises for the current workout
     */
    private fun fetchWorkoutExercises(workoutId: String) {
        viewModelScope.launch {
            getWorkoutExercisesUseCase(workoutId)
                .catch { exception ->
                    handleError(exception)
                }
                .collect { exercises ->
                    _uiState.update {
                        it.copy(
                            exercises = exercises,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Adds a new exercise to the workout
     */
    fun addExerciseToWorkout(
        workoutId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = addExerciseToWorkoutUseCase(
                    workoutId,
                    exerciseId,
                    sets,
                    reps
                )

                result.onSuccess { newExercise ->
                    _uiState.update { state ->
                        state.copy(
                            exercises = state.exercises + newExercise,
                            isCreateExerciseDrawerVisible = false,
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Updates an existing workout exercise
     */
    fun updateExerciseInWorkout(
        workoutId: String,
        workoutExerciseId: String,
        exerciseId: String,
        sets: Int,
        reps: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = updateWorkoutExerciseUseCase(
                    workoutExerciseId,
                    sets,
                    reps
                )

                result.onSuccess { updatedExercise ->
                    _uiState.update { state ->
                        state.copy(
                            exercises = state.exercises.map {
                                if (it.id == updatedExercise.id) updatedExercise else it
                            },
                            isUpdateExerciseDrawerVisible = false,
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Removes an exercise from the workout
     */
    fun removeExerciseFromWorkout(workoutExerciseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = deleteWorkoutExerciseUseCase(workoutExerciseId)

                result.onSuccess { success ->
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                exercises = state.exercises.filter { it.id != workoutExerciseId },
                                isDeleteConfirmationVisible = false,
                                isLoading = false
                            )
                        }
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Reorders an exercise within the workout
     */
    fun reorderExercise(workoutExerciseId: String, newPosition: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = reorderWorkoutExerciseUseCase(workoutExerciseId, newPosition)

                result.onSuccess { success ->
                    if (success) {
                        // Refresh exercises to get updated positions
                        currentWorkoutId?.let { fetchWorkoutExercises(it) }
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Starts a workout session
     */
    fun startWorkoutSession(workoutId: String, onStartWorkoutSession: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // In a real implementation, this would call a use case to start a workout session
                // For now, we'll just simulate it by returning a placeholder session ID
                // Replace this with actual implementation that calls the appropriate use case
                val sessionId = "session-$workoutId-${System.currentTimeMillis()}"
                onStartWorkoutSession(sessionId)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // UI state handlers
    fun setSelectedWorkoutExercise(exercise: WorkoutExercise) {
        _uiState.update { it.copy(selectedExercise = exercise) }
    }

    fun showCreateWorkoutExerciseDrawer() {
        _uiState.update { it.copy(isCreateExerciseDrawerVisible = true) }
    }

    fun hideCreateWorkoutExerciseDrawerVisible() {
        _uiState.update { it.copy(isCreateExerciseDrawerVisible = false) }
    }

    fun showUpdateWorkoutExerciseDrawer() {
        _uiState.update { it.copy(isUpdateExerciseDrawerVisible = true) }
    }

    fun hideUpdateWorkoutExerciseDrawer() {
        _uiState.update { it.copy(isUpdateExerciseDrawerVisible = false) }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Centralized error handling
     */
    private fun handleError(error: Throwable) {
        Timber.e(error, "Error in WorkoutExercisesViewModel")

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
                "An unexpected error occurred: ${error.message ?: "Unknown error"}"
        }

        _uiState.update {
            it.copy(
                errorMessage = errorMessage,
                isLoading = false
            )
        }
    }
}