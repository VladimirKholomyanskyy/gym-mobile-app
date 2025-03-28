package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Unified UI state for the training programs screen
 */
data class TrainingProgramsUiState(
    val programs: List<TrainingProgram> = emptyList(),
    val selectedProgram: TrainingProgram? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCreateProgramDrawerVisible: Boolean = false,
    val isUpdateProgramDrawerVisible: Boolean = false,
    val isDeleteConfirmationVisible: Boolean = false
)

@HiltViewModel
class TrainingProgramsViewModel @Inject constructor(
    private val createUseCase: CreateTrainingProgramUseCase,
    private val updateUseCase: UpdateTrainingProgramUseCase,
    private val deleteUseCase: DeleteTrainingProgramUseCase,
    private val getTrainingProgramsUseCase: GetTrainingProgramsUseCase,
    private val profileManager: ProfileManager
) : ViewModel() {

    // Unified UI state
    private val _uiState = MutableStateFlow(TrainingProgramsUiState())
    val uiState: StateFlow<TrainingProgramsUiState> = _uiState.asStateFlow()

    init {
        fetchTrainingPrograms()
    }

    /**
     * Fetches training programs from the repository
     */
    fun fetchTrainingPrograms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val profileId = profileManager.getCurrentProfileId() ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "User profile not found"
                        )
                    }
                    return@launch
                }

                getTrainingProgramsUseCase(profileId)
                    .catch { exception ->
                        handleError(exception)
                    }
                    .collect { programs ->
                        _uiState.update {
                            it.copy(
                                programs = programs,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Creates a new training program
     */
    fun createTrainingProgram(name: String, description: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val profileId = profileManager.getCurrentProfileId() ?: run {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "User profile not found"
                        )
                    }
                    return@launch
                }

                // Validate input on viewmodel level for better UX
                validateProgramInput(name, description)?.let { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error
                        )
                    }
                    return@launch
                }

                val result = createUseCase(profileId, name, description)

                result.onSuccess { program ->
                    _uiState.update { state ->
                        state.copy(
                            programs = state.programs + program,
                            isCreateProgramDrawerVisible = false,
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
     * Updates an existing training program
     */
    fun updateTrainingProgram(
        programId: String,
        name: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Validate input on viewmodel level
                name?.let {
                    validateProgramInput(it, description)?.let { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error
                            )
                        }
                        return@launch
                    }
                }

                val result = updateUseCase(programId, name, description)

                result.onSuccess { updatedProgram ->
                    _uiState.update { state ->
                        state.copy(
                            programs = state.programs.map {
                                if (it.id == updatedProgram.id) updatedProgram else it
                            },
                            isUpdateProgramDrawerVisible = false,
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
     * Deletes a training program
     */
    fun deleteTrainingProgram(programId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = deleteUseCase(programId)

                result.onSuccess { success ->
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                programs = state.programs.filter { it.id != programId },
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

    // UI state management methods
    fun setSelectedProgram(program: TrainingProgram) {
        _uiState.update { it.copy(selectedProgram = program) }
    }

    fun showCreateProgramDrawer() {
        _uiState.update { it.copy(isCreateProgramDrawerVisible = true) }
    }

    fun hideCreateProgramDrawer() {
        _uiState.update { it.copy(isCreateProgramDrawerVisible = false) }
    }

    fun showUpdateProgramDrawer() {
        _uiState.update { it.copy(isUpdateProgramDrawerVisible = true) }
    }

    fun hideUpdateProgramDrawer() {
        _uiState.update { it.copy(isUpdateProgramDrawerVisible = false) }
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
        Timber.e(error, "Error in TrainingProgramsViewModel")

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