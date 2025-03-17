package com.neyra.gymapp.domain

import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.data.preferences.UserPreferences
import com.neyra.gymapp.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    private val userPreferences: UserPreferences,
    private val profileRepository: ProfileRepository
) {
    val profileIdFlow: Flow<UUID?> = userPreferences.profileIdFlow

    suspend fun getProfile(): ProfileEntity? {
        val profileId = userPreferences.profileIdFlow.firstOrNull()
        return profileId?.let { profileRepository.getProfile(it) }
    }

    suspend fun setProfileId(profileId: UUID) {
        userPreferences.saveProfileId(profileId)
    }

    suspend fun getCurrentProfileId(): UUID? {
        return profileIdFlow.firstOrNull()
    }
}