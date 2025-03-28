package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.usecase.CreateWorkoutUseCase
import com.neyra.gymapp.domain.usecase.DeleteWorkoutUseCase
import com.neyra.gymapp.domain.usecase.GetWorkoutsUseCase
import com.neyra.gymapp.domain.usecase.ReorderWorkoutUseCase
import com.neyra.gymapp.domain.usecase.UpdateWorkoutUseCase
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
 * Unified UI state for the workouts screen
 */
data class WorkoutsUiState(
    val workouts: List<Workout> = emptyList(),
    val selectedWorkout: Workout? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCreateWorkoutDrawerVisible: Boolean = false,
    val isUpdateWorkoutDrawerVisible: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val createWorkoutUseCase: CreateWorkoutUseCase,
    private val updateWorkoutUseCase: UpdateWorkoutUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val reorderWorkoutUseCase: ReorderWorkoutUseCase
) : ViewModel() {

    // Unified UI state
    private val _uiState = MutableStateFlow(WorkoutsUiState())
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    // Current program ID being displayed
    private var currentProgramId: String? = null

    /**
     * Fetches workouts for the given training program
     */
    fun fetchWorkouts(programId: String) {
        currentProgramId = programId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getWorkoutsUseCase(programId)
                .catch { exception ->
                    handleError(exception)
                }
                .collect { workouts ->
                    _uiState.update {
                        it.copy(
                            workouts = workouts,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Creates a new workout in the current training program
     */
    fun createWorkout(programId: String, workoutName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = createWorkoutUseCase(programId, workoutName)

                result.onSuccess { workout ->
                    _uiState.update { state ->
                        state.copy(
                            workouts = state.workouts + workout,
                            isCreateWorkoutDrawerVisible = false,
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
     * Updates an existing workout
     */
    fun updateWorkout(programId: String, workoutId: String, workoutName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = updateWorkoutUseCase(workoutId, workoutName)

                result.onSuccess { updatedWorkout ->
                    _uiState.update { state ->
                        state.copy(
                            workouts = state.workouts.map {
                                if (it.id == updatedWorkout.id) updatedWorkout else it
                            },
                            isUpdateWorkoutDrawerVisible = false,
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
     * Deletes a workout
     */
    fun deleteWorkout(programId: String, workoutId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = deleteWorkoutUseCase(workoutId)

                result.onSuccess { success ->
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                workouts = state.workouts.filter { it.id != workoutId },
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
     * Reorders a workout within the program
     */
    fun reorderWorkout(workoutId: String, newPosition: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = reorderWorkoutUseCase(workoutId, newPosition)

                result.onSuccess { success ->
                    if (success) {
                        currentProgramId?.let { fetchWorkouts(it) }
                    }
                }.onFailure { error ->
                    handleError(error)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    // UI state handlers
    fun setSelectedWorkout(workout: Workout) {
        _uiState.update { it.copy(selectedWorkout = workout) }
    }

    fun showCreateWorkoutDrawer() {
        _uiState.update { it.copy(isCreateWorkoutDrawerVisible = true) }
    }

    fun hideCreateWorkoutDrawer() {
        _uiState.update { it.copy(isCreateWorkoutDrawerVisible = false) }
    }

    fun showUpdateWorkoutDrawer() {
        _uiState.update { it.copy(isUpdateWorkoutDrawerVisible = true) }
    }

    fun hideUpdateWorkoutDrawer() {
        _uiState.update { it.copy(isUpdateWorkoutDrawerVisible = false) }
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
        Timber.e(error, "Error in WorkoutsViewModel")

        val errorMessage = when (error) {
            is DomainError.ValidationError.InvalidName ->
                "Invalid workout name: ${error.message}"

            is DomainError.NetworkError.NoConnection ->
                "No internet connection. Please check your network."

            is DomainError.AuthenticationError.Unauthorized ->
                "You are not authorized. Please log in again."

            is DomainError.DataError.NotFound ->
                "The requested workout was not found."

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