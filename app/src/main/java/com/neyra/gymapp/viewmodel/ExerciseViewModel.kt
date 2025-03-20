package com.neyra.gymapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.openapi.apis.ExercisesApi
import com.neyra.gymapp.openapi.models.Exercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exercisesApi: ExercisesApi
) : ViewModel() {

    private val _exercises = MutableStateFlow<UiState<List<Exercise>>>(UiState.Loading)
    val exercises: StateFlow<UiState<List<Exercise>>> = _exercises

    fun fetchExercises() {
        viewModelScope.launch {
            try {
                val response = exercisesApi.listExercises(1, 10)
                Log.d("ExerciseViewModel", "Exercises fetched successfully $response")
                if (response.isSuccessful) {

                    _exercises.value = UiState.Success(response.body()?.items ?: emptyList())
                } else {
                    _exercises.value = UiState.Error("Failed to fetch exercises")
                }
            } catch (e: Exception) {
                _exercises.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
