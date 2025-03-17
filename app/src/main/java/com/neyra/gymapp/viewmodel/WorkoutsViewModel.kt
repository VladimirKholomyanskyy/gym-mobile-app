package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.common.UiState
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val createWorkoutUseCase: CreateWorkoutUseCase,
    private val updateWorkoutUseCase: UpdateWorkoutUseCase,
    private val deleteWorkoutUseCase: DeleteWorkoutUseCase,
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
    private val reorderWorkoutUseCase: ReorderWorkoutUseCase
) : ViewModel() {

    private val _workouts = MutableStateFlow<UiState<List<Workout>>>(UiState.Loading)
    val workouts: StateFlow<UiState<List<Workout>>> = _workouts.asStateFlow()

    private val _selectedWorkout = MutableStateFlow<Workout?>(null)
    val selectedWorkout: StateFlow<Workout?> = _selectedWorkout.asStateFlow()

    private val _isCreateWorkoutDrawerVisible = MutableStateFlow(false)
    val isCreateWorkoutDrawerVisible: StateFlow<Boolean> =
        _isCreateWorkoutDrawerVisible.asStateFlow()

    private val _isUpdateWorkoutDrawerVisible = MutableStateFlow(false)
    val isUpdateWorkoutDrawerVisible: StateFlow<Boolean> =
        _isUpdateWorkoutDrawerVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentProgramId: String? = null

    fun fetchWorkouts(programId: String) {
        currentProgramId = programId
        viewModelScope.launch {
            _isLoading.value = true
            _workouts.value = UiState.Loading

            getWorkoutsUseCase(programId)
                .catch { exception ->
                    handleError(exception)
                }
                .collect { workouts ->
                    _workouts.value = UiState.Success(workouts)
                    _isLoading.value = false
                }
        }
    }

    fun createWorkout(programId: String, workoutName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = createWorkoutUseCase(programId, workoutName)

                result.onSuccess { workout ->
                    _isCreateWorkoutDrawerVisible.value = false

                    // Update workouts list
                    when (val currentWorkouts = _workouts.value) {
                        is UiState.Success -> {
                            _workouts.value = UiState.Success(currentWorkouts.data + workout)
                        }

                        else -> fetchWorkouts(programId) // Reload all workouts if current state is not Success
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

    fun updateWorkout(programId: String, workoutId: String, workoutName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = updateWorkoutUseCase(workoutId, workoutName)

                result.onSuccess { updatedWorkout ->
                    _isUpdateWorkoutDrawerVisible.value = false

                    // Update the workouts list with the updated workout
                    when (val currentWorkouts = _workouts.value) {
                        is UiState.Success -> {
                            val updatedList = currentWorkouts.data.map {
                                if (it.id == updatedWorkout.id) updatedWorkout else it
                            }
                            _workouts.value = UiState.Success(updatedList)
                        }

                        else -> fetchWorkouts(programId) // Reload all workouts if current state is not Success
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

    fun deleteWorkout(programId: String, workoutId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = deleteWorkoutUseCase(workoutId)

                result.onSuccess { success ->
                    if (success) {
                        // Update the workouts list by removing the deleted workout
                        when (val currentWorkouts = _workouts.value) {
                            is UiState.Success -> {
                                val updatedList = currentWorkouts.data.filter { it.id != workoutId }
                                _workouts.value = UiState.Success(updatedList)
                            }

                            else -> fetchWorkouts(programId) // Reload all workouts if current state is not Success
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

    fun reorderWorkout(workoutId: String, newPosition: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = reorderWorkoutUseCase(workoutId, newPosition)

                result.onSuccess { success ->
                    if (success) {
                        // Refresh the workouts list after reordering
                        currentProgramId?.let { fetchWorkouts(it) }
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

    fun setSelectedWorkout(workout: Workout) {
        _selectedWorkout.value = workout
    }

    fun showCreateWorkoutDrawer() {
        _isCreateWorkoutDrawerVisible.value = true
    }

    fun hideCreateWorkoutDrawer() {
        _isCreateWorkoutDrawerVisible.value = false
    }

    fun showUpdateWorkoutDrawer() {
        _isUpdateWorkoutDrawerVisible.value = true
    }

    fun hideUpdateWorkoutDrawer() {
        _isUpdateWorkoutDrawerVisible.value = false
    }

    private fun handleError(error: Throwable) {
        _isLoading.value = false
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
                "An unexpected error occurred: ${error.message}"
        }
        _errorMessage.value = errorMessage
        _workouts.value = UiState.Error(errorMessage)
    }
}