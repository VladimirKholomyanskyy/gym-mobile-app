package com.neyra.gymapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.common.SyncState
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.data.mapper.toModels
import com.neyra.gymapp.data.repository.TrainingProgramRepository
import com.neyra.gymapp.data.sync.SyncManager
import com.neyra.gymapp.domain.ProfileManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingProgramsViewModel @Inject constructor(
    private val trainingProgramRepository: TrainingProgramRepository,
    private val syncManager: SyncManager,
    private val profileManager: ProfileManager // Added to get current profile ID
) : ViewModel() {

    // Holds the list of training programs.
    private val _programs = MutableStateFlow<List<TrainingProgramEntity>>(emptyList())
    val programs: StateFlow<List<TrainingProgramEntity>> = _programs

    // Converted to UI models for the view
    val uiPrograms = _programs.map { entities ->
        entities.toModels() // You'll need to create this mapper function
    }

    private val _selectedProgram = MutableStateFlow<TrainingProgramEntity?>(null)
    val selectedProgram: StateFlow<TrainingProgramEntity?> = _selectedProgram

    // Optionally track loading and error states.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _isCreateProgramDrawerVisible = MutableStateFlow(false)
    val isCreateProgramDrawerVisible: StateFlow<Boolean> = _isCreateProgramDrawerVisible

    private val _isUpdateProgramDrawerVisible = MutableStateFlow(false)
    val isUpdateProgramDrawerVisible: StateFlow<Boolean> = _isUpdateProgramDrawerVisible

    // Track offline indicator
    private val _hasPendingSync = MutableStateFlow(false)
    val hasPendingSync: StateFlow<Boolean> = _hasPendingSync

    init {
        observeTrainingPrograms()
        observePendingSyncs()
        refreshTrainingPrograms()
    }

    private fun observeTrainingPrograms() {
        viewModelScope.launch {
            profileManager.getCurrentProfileId()?.let { profileId ->
                trainingProgramRepository.getTrainingPrograms(profileId)
                    .catch { e ->
                        // Handle error
                        _isLoading.value = false
                    }
                    .collect { programs ->
                        _programs.value = programs
                        _isLoading.value = false
                    }
            }
        }
    }

    private fun observePendingSyncs() {
        viewModelScope.launch {
            trainingProgramRepository.getPendingSyncPrograms()
                .collect { pendingPrograms ->
                    _hasPendingSync.value = pendingPrograms.isNotEmpty()
                }
        }
    }

    fun setSelectedProgram(program: TrainingProgramEntity) {
        _selectedProgram.value = program
    }

    fun refreshTrainingPrograms() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncState.value = SyncState.Syncing

            try {
                val result = trainingProgramRepository.refreshTrainingPrograms()
                if (result.isFailure) {
                    _syncState.value =
                        SyncState.Failed(result.exceptionOrNull()?.message ?: "Sync failed")
                    // Try background sync
                    syncManager.requestImmediateSync()
                } else {
                    _syncState.value = SyncState.Success
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Failed(e.message ?: "Sync failed")
                syncManager.requestImmediateSync()
            } finally {
                _isLoading.value = false
            }
        }
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

    fun createTrainingProgram(name: String, description: String?) {
        viewModelScope.launch {
            try {
                profileManager.getCurrentProfileId()?.let { profileId ->
                    val result = trainingProgramRepository.createTrainingProgram(
                        profileId,
                        name,
                        description
                    )
                    if (result.isSuccess) {
                        _isCreateProgramDrawerVisible.value = false
                    } else {
                        // Handle error
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle exceptions
            }
        }
    }

    fun updateTrainingProgram(programId: String, name: String?, description: String?) {
        viewModelScope.launch {
            try {
                val result =
                    trainingProgramRepository.updateTrainingProgram(programId, name, description)
                if (result.isSuccess) {
                    _isUpdateProgramDrawerVisible.value = false
                } else {
                    // Handle error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle exceptions
            }
        }
    }

    fun deleteTrainingProgram(programId: String) {
        viewModelScope.launch {
            try {
                val result = trainingProgramRepository.deleteTrainingProgram(programId)
                if (result.isFailure) {
                    // Handle error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle exceptions
            }
        }
    }

    fun syncPendingChanges() {
        syncManager.requestImmediateSync()
    }
}