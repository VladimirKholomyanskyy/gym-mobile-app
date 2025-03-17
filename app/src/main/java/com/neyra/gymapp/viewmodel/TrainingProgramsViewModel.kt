package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.domain.ProfileManager
import com.neyra.gymapp.domain.error.DomainError
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.domain.usecase.CreateTrainingProgramUseCase
import com.neyra.gymapp.domain.usecase.DeleteTrainingProgramUseCase
import com.neyra.gymapp.domain.usecase.GetTrainingProgramsUseCase
import com.neyra.gymapp.domain.usecase.UpdateTrainingProgramUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingProgramsViewModel @Inject constructor(
    private val createUseCase: CreateTrainingProgramUseCase,
    private val updateUseCase: UpdateTrainingProgramUseCase,
    private val deleteUseCase: DeleteTrainingProgramUseCase,
    private val getTrainingProgramsUseCase: GetTrainingProgramsUseCase,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _programs = MutableStateFlow<UiState<List<TrainingProgram>>>(UiState.Loading)
    val programs: StateFlow<UiState<List<TrainingProgram>>> = _programs.asStateFlow()

    private val _selectedProgram = MutableStateFlow<TrainingProgram?>(null)
    val selectedProgram: StateFlow<TrainingProgram?> = _selectedProgram.asStateFlow()

    private val _isCreateProgramDrawerVisible = MutableStateFlow(false)
    val isCreateProgramDrawerVisible: StateFlow<Boolean> =
        _isCreateProgramDrawerVisible.asStateFlow()

    private val _isUpdateProgramDrawerVisible = MutableStateFlow(false)
    val isUpdateProgramDrawerVisible: StateFlow<Boolean> =
        _isUpdateProgramDrawerVisible.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchTrainingPrograms()
    }

    fun fetchTrainingPrograms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Assuming you have a way to get the current profile ID
                val profileId = profileManager.getCurrentProfileId()

                if (profileId != null) {
                    getTrainingProgramsUseCase(profileId)
                        .catch { exception ->
                            handleError(exception)
                        }
                        .collect { programs ->
                            _programs.value = UiState.Success(programs)
                            _isLoading.value = false
                        }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun createTrainingProgram(name: String, description: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val profileId = profileManager.getCurrentProfileId()
                val result = profileId?.let { createUseCase(it, name, description) }

                if (result != null) {
                    result.onSuccess {
                        _isCreateProgramDrawerVisible.value = false
                        fetchTrainingPrograms() // Refresh the list
                    }.onFailure { error ->
                        handleError(error)
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTrainingProgram(
        programId: String,
        name: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = updateUseCase(programId, name, description)

                result.onSuccess {
                    _isUpdateProgramDrawerVisible.value = false
                    fetchTrainingPrograms() // Refresh the list
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

    fun deleteTrainingProgram(programId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = deleteUseCase(programId)

                result.onSuccess {
                    fetchTrainingPrograms() // Refresh the list
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

    fun setSelectedProgram(program: TrainingProgram) {
        _selectedProgram.value = program
    }

    fun showCreateProgramDrawer() {
        _isCreateProgramDrawerVisible.value = true
    }

    fun hideCreateProgramDrawer() {
        _isCreateProgramDrawerVisible.value = false
    }

    fun showUpdateProgramDrawer() {
        _isUpdateProgramDrawerVisible.value = true
    }

    fun hideUpdateProgramDrawer() {
        _isUpdateProgramDrawerVisible.value = false
    }

    private fun handleError(error: Throwable) {
        _isLoading.value = false
        val errorMessage = when (error) {
            is DomainError.ValidationError.InvalidName ->
                "Invalid program name: ${error.message}"

            is DomainError.NetworkError.NoConnection ->
                "No internet connection. Please check your network."

            is DomainError.AuthenticationError.Unauthorized ->
                "You are not authorized. Please log in again."

            is DomainError ->
                error.message

            else ->
                "An unexpected error occurred: ${error.message}"
        }
        _errorMessage.value = errorMessage
        _programs.value = UiState.Error(errorMessage)
    }


}