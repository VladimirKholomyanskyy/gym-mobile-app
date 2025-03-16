package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.openapi.apis.ExerciseLogsApi
import com.neyra.gymapp.openapi.apis.ExercisesApi
import com.neyra.gymapp.openapi.models.Exercise
import com.neyra.gymapp.openapi.models.LogExerciseResponse
import com.neyra.gymapp.openapi.models.WeightPerDayResponseTotalWeightPerDayInner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailsViewModel @Inject constructor(
    private val exercisesApi: ExercisesApi,
    private val exerciseLogsApi: ExerciseLogsApi
) : ViewModel() {

    private val _exercise = MutableStateFlow<UiState<Exercise>>(UiState.Loading)
    val exercise: StateFlow<UiState<Exercise>> = _exercise

    // New state for exercise logs
    private val _exerciseLogs =
        MutableStateFlow<UiState<List<LogExerciseResponse>>>(UiState.Loading)
    val exerciseLogs: StateFlow<UiState<List<LogExerciseResponse>>> = _exerciseLogs

    private val _exerciseProgress =
        MutableStateFlow<UiState<List<WeightPerDayResponseTotalWeightPerDayInner>>>(UiState.Loading)
    val exerciseProgress: StateFlow<UiState<List<WeightPerDayResponseTotalWeightPerDayInner>>> =
        _exerciseProgress

    fun fetchExercise(exerciseId: String) {
        viewModelScope.launch {
            try {
                val response = exercisesApi.getExerciseById(exerciseId)
                if (response.isSuccessful) {
                    val exercise = response.body()
                    _exercise.value = if (exercise != null) {
                        UiState.Success(exercise)
                    } else {
                        UiState.Error("Exercise not found")
                    }
                } else {
                    _exercise.value = UiState.Error("Failed to fetch exercise")
                }
            } catch (e: Exception) {
                _exercise.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchExerciseLogs(exerciseId: String, workoutSessionId: String? = null) {
        viewModelScope.launch {
            try {
                val response = exerciseLogsApi.listExerciseLogs(exerciseId)
                if (response.isSuccessful) {
                    val logs = response.body() ?: emptyList()
                    _exerciseLogs.value = UiState.Success(logs)
                } else {
                    _exerciseLogs.value = UiState.Error("Failed to fetch exercise logs")
                }
            } catch (e: Exception) {
                _exerciseLogs.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchExerciseProgress(exerciseId: String) {
        viewModelScope.launch {
            try {
                val response = exerciseLogsApi.getWeightPerDay(exerciseId)
                if (response.isSuccessful) {
                    val progress = response.body()?.totalWeightPerDay ?: emptyList()
                    _exerciseProgress.value = UiState.Success(progress)
                } else {
                    _exerciseProgress.value = UiState.Error("Failed to fetch exercise progress")
                }
            } catch (e: Exception) {
                _exerciseProgress.value = UiState.Error(e.message ?: "Unknown error")
            }

        }
    }
}
