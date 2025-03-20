package com.neyra.gymapp.domain

import android.util.Log
import com.neyra.gymapp.data.dao.ProfileDao
import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.data.mapper.toEntity
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.preferences.UserPreferences
import com.neyra.gymapp.openapi.apis.ProfileApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    private val userPreferences: UserPreferences,
    private val profileDao: ProfileDao,
    private val profileApi: ProfileApi,
    private val networkManager: NetworkManager
) {
    // Flow to observe profile changes
    private val _currentProfile = MutableStateFlow<ProfileEntity?>(null)
    val currentProfile: StateFlow<ProfileEntity?> = _currentProfile

    // Get current profile with fetch if needed
    suspend fun getCurrentProfile(forceRefresh: Boolean = false): ProfileEntity? {
        // Try to get from memory first
        _currentProfile.value?.let {
            if (!forceRefresh) return it
        }

        // Try to get stored profile ID
        val profileId = userPreferences.getProfileId()

        // If we have a stored profile ID, try to get from local DB
        if (profileId != null && !forceRefresh) {
            val localProfile = profileDao.getProfileById(profileId)
            if (localProfile != null) {
                _currentProfile.value = localProfile
                return localProfile
            }
        }

        // If we don't have a profile or need refresh, fetch from server
        return fetchProfileFromServer()
    }

    private suspend fun fetchProfileFromServer(): ProfileEntity? {
        if (!networkManager.isOnline()) return null

        try {
            // The API doesn't need ID - it extracts it from JWT token

            val response = profileApi.getProfile()

            if (response.isSuccessful && response.body() != null) {
                // Convert API model to entity
                val profileEntity = response.body()!!.toEntity()
                // Save to local DB
                profileDao.insertProfile(profileEntity)

                // Save profile ID to preferences
                userPreferences.saveProfileId(profileEntity.id)

                // Update memory cache
                _currentProfile.value = profileEntity

                return profileEntity
            }
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to fetch profile: ${e.message}")
        }

        return null
    }

    // For use by repos that just need the ID
    suspend fun getCurrentProfileId(fetchIfNeeded: Boolean = true): String? {
        // Try from preferences first
        val profileId = userPreferences.getProfileId()
        if (profileId != null) {
            return profileId
        }

        // If allowed to fetch and we don't have an ID, get the profile
        if (fetchIfNeeded) {
            return getCurrentProfile()?.id
        }

        return null
    }

    suspend fun clearProfile() {
        _currentProfile.value = null
        userPreferences.clearProfileId()
    }

    suspend fun syncProfile() {
        if (!networkManager.isOnline()) return

        try {
            fetchProfileFromServer()
        } catch (e: Exception) {
            Log.e("ProfileManager", "Failed to sync profile: ${e.message}")
        }
    }
}