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

    // Program list state
    private val _programs = MutableStateFlow<UiState<List<TrainingProgram>>>(UiState.Loading)
    val programs: StateFlow<UiState<List<TrainingProgram>>> = _programs.asStateFlow()

    // Selected program for edit/delete operations
    private val _selectedProgram = MutableStateFlow<TrainingProgram?>(null)
    val selectedProgram: StateFlow<TrainingProgram?> = _selectedProgram.asStateFlow()

    // UI state for drawers
    private val _isCreateProgramDrawerVisible = MutableStateFlow(false)
    val isCreateProgramDrawerVisible: StateFlow<Boolean> =
        _isCreateProgramDrawerVisible.asStateFlow()

    private val _isUpdateProgramDrawerVisible = MutableStateFlow(false)
    val isUpdateProgramDrawerVisible: StateFlow<Boolean> =
        _isUpdateProgramDrawerVisible.asStateFlow()

    private val _isDeleteConfirmationVisible = MutableStateFlow(false)
    val isDeleteConfirmationVisible: StateFlow<Boolean> = _isDeleteConfirmationVisible.asStateFlow()

    // Loading and error states
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
            _programs.value = UiState.Loading
            try {
                val profileId = profileManager.getCurrentProfileId() ?: return@launch

                getTrainingProgramsUseCase(profileId)
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collect { programs ->
                        _programs.value = UiState.Success(programs)
                        _isLoading.value = false
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
                val profileId = profileManager.getCurrentProfileId() ?: run {
                    _errorMessage.value = "User profile not found"
                    _isLoading.value = false
                    return@launch
                }

                // Validate input on viewmodel level for better UX
                validateProgramInput(name, description)
                    ?.let { error ->
                        _errorMessage.value = error
                        _isLoading.value = false
                        return@launch
                    }

                val result = createUseCase(profileId, name, description)

                result.onSuccess {
                    _isCreateProgramDrawerVisible.value = false
                    refreshProgramsList()
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

    fun updateTrainingProgram(
        programId: String,
        name: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Validate input on viewmodel level
                name?.let {
                    validateProgramInput(it, description)
                        ?.let { error ->
                            _errorMessage.value = error
                            _isLoading.value = false
                            return@launch
                        }
                }
                val result = updateUseCase(programId, name, description)

                result.onSuccess {
                    _isUpdateProgramDrawerVisible.value = false
                    refreshProgramsList()
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
                    _isDeleteConfirmationVisible.value = false
                    refreshProgramsList()
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

    // Helper function for validating program inputs
    private fun validateProgramInput(name: String, description: String?): String? {
        if (name.isBlank()) {
            return "Program name cannot be empty"
        }

        if (name.length > TrainingProgram.MAX_NAME_LENGTH) {
            return "Name must be ${TrainingProgram.MAX_NAME_LENGTH} characters or less"
        }

        if (description != null && description.length > TrainingProgram.MAX_DESCRIPTION_LENGTH) {
            return "Description must be ${TrainingProgram.MAX_DESCRIPTION_LENGTH} characters or less"
        }

        return null
    }

    // Helper function to refresh programs list after changes
    private fun refreshProgramsList() {
        fetchTrainingPrograms()
    }

    // Dialog and drawer state management
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

    fun showDeleteConfirmation() {
        _isDeleteConfirmationVisible.value = true
    }

    fun hideDeleteConfirmation() {
        _isDeleteConfirmationVisible.value = false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun handleError(error: Throwable) {
        _isLoading.value = false
        val errorMessage = when (error) {
            is DomainError.ValidationError.InvalidName ->
                "Invalid program name: ${error.message}"

            is DomainError.ValidationError.InvalidDescription ->
                "Invalid description: ${error.message}"

            is DomainError.ValidationError.WorkoutLimitExceeded ->
                "You've reached the maximum number of workouts for this program"

            is DomainError.NetworkError.NoConnection ->
                "No internet connection. Please check your network."

            is DomainError.AuthenticationError.Unauthorized ->
                "You are not authorized. Please log in again."

            is DomainError.DataError.NotFound ->
                "The requested program was not found"

            is DomainError.DataError.DuplicateEntry ->
                "A program with this name already exists"

            is DomainError ->
                error.message

            else ->
                "An unexpected error occurred: ${error.message}"
        }
        _errorMessage.value = errorMessage
        _programs.value = when (_programs.value) {
            is UiState.Loading -> UiState.Error(errorMessage)
            is UiState.Success -> _programs.value // Keep current data but show error in snackbar
            is UiState.Error -> UiState.Error(errorMessage)
        }
    }
}