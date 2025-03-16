package com.neyra.gymapp.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.openapi.apis.ExerciseLogsApi
import com.neyra.gymapp.openapi.apis.WorkoutSessionsApi
import com.neyra.gymapp.openapi.models.LogExerciseRequest
import com.neyra.gymapp.openapi.models.WorkoutSessionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val exerciseLogsApi: ExerciseLogsApi,
    private val workoutSessionsApi: WorkoutSessionsApi
) : ViewModel() {

    private val _workoutSession = MutableStateFlow<WorkoutSessionResponse?>(null)
    val workoutSession: StateFlow<WorkoutSessionResponse?> = _workoutSession

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchSession(sessionId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response: Response<WorkoutSessionResponse> =
                    workoutSessionsApi.getWorkoutSession(sessionId)
                if (response.isSuccessful) {
                    _workoutSession.value = response.body()
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

    fun completeSession(
        sessionId: String,
        exerciseInputs: Map<String, List<SetInput>>
    ) {
        viewModelScope.launch {
            exerciseInputs.forEach { (exerciseId, setInputs) ->
                setInputs.forEachIndexed { index, setInput ->
                    // Convert text input to integers (add proper error handling as needed)
                    val reps = setInput.reps.value.toIntOrNull() ?: 0
                    val weight = setInput.weight.value.toIntOrNull() ?: 0

                    // Build your API request.
                    val request = LogExerciseRequest(
                        workoutSessionId = sessionId,
                        exerciseId = exerciseId,
                        setNumber = index + 1,
                        repsCompleted = reps,
                        weightUsed = weight
                    )

                    // Call the API (handle the response/error as needed).
                    val response = exerciseLogsApi.logExercise(request)
                    if (response.isSuccessful) {
                        // Optionally, update UI state or log success.
                    } else {
                        // Handle API error.
                    }
                }
            }
            workoutSessionsApi.completeWorkoutSession(sessionId)
        }
    }

}

class SetInput(
    reps: String = "",
    weight: String = ""
) {
    var reps = mutableStateOf(reps)
    var weight = mutableStateOf(weight)
}