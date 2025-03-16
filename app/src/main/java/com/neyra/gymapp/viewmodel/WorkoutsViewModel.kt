package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import com.neyra.gymapp.openapi.models.WorkoutRequest
import com.neyra.gymapp.openapi.models.WorkoutResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val workoutsApi: WorkoutsApi
) : ViewModel() {

    // Holds the list of workouts fetched from the API.
    private val _workouts = MutableStateFlow<List<WorkoutResponse>>(emptyList())
    val workouts: StateFlow<List<WorkoutResponse>> = _workouts

    private val _selectedWorkout = MutableStateFlow<WorkoutResponse?>(null)
    val selectedWorkout: StateFlow<WorkoutResponse?> = _selectedWorkout

    // Optionally, track loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isCreateWorkoutDrawerVisible = MutableStateFlow(false)
    val isCreateWorkoutDrawerVisible: StateFlow<Boolean> = _isCreateWorkoutDrawerVisible

    private val _isUpdateWorkoutDrawerVisible = MutableStateFlow(false)
    val isUpdateWorkoutDrawerVisible: StateFlow<Boolean> = _isUpdateWorkoutDrawerVisible

    fun setSelectedWorkout(workout: WorkoutResponse) {
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

    // Function to fetch workouts for a given program
    fun fetchWorkouts(programId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response: Response<List<WorkoutResponse>> =
                    workoutsApi.listWorkoutsForProgram(programId)
                if (response.isSuccessful) {
                    _workouts.value = response.body() ?: emptyList()
                } else {
                    // Handle error response here (e.g., log error, set an error state)
                }
            } catch (e: Exception) {
                // Handle exception (network error, etc.)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createWorkout(programId: String, workoutName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val request = WorkoutRequest(workoutName)
                val response: Response<WorkoutResponse> =
                    workoutsApi.addWorkoutToProgram(programId, request)
                if (response.isSuccessful) {
                    response.body()?.let { newWorkout ->
                        // Add the newly created program to the list
                        _workouts.value += newWorkout
                        _isCreateWorkoutDrawerVisible.value = false
                    }

                } else {
                    //handle
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateWorkout(programId: String, workoutId: String, workoutName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val request = WorkoutRequest(workoutName)
                val response: Response<WorkoutResponse> =
                    workoutsApi.updateWorkout(programId, workoutId, request)
                if (response.isSuccessful) {
                    response.body()?.let { updatedWorkout ->
                        // Add the newly created program to the list
                        _workouts.value = _workouts.value.filter { it.id != workoutId }
                        _workouts.value += updatedWorkout
                        _isUpdateWorkoutDrawerVisible.value = false
                    }


                } else {
                    //handle
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteWorkout(programId: String, workoutId: String) {
        viewModelScope.launch {
            try {
                val response = workoutsApi.deleteWorkout(programId, workoutId)

                if (response.isSuccessful) {
                    _workouts.value = _workouts.value.filter { it.id != workoutId }
                } else {
                    // Handle API error (e.g., show a Toast or error message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle network or other exceptions
            }
        }
    }
}