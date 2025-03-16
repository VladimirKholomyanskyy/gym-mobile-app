package com.neyra.gymapp.data.auth

import com.neyra.gymapp.data.api.AuthApi
import com.neyra.gymapp.data.api.models.AuthResponse
import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.data.dao.ProfileDao
import com.neyra.gymapp.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

@Singleton
class AuthManager @Inject constructor(
    private val authApi: AuthApi,
    private val profileDao: ProfileDao,
    private val preferencesManager: PreferencesManager
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: Flow<AuthState> = _authState

    // Initialize auth state based on stored token
    suspend fun initialize() {
        val token = preferencesManager.getAuthToken().first()
        if (token != null) {
            val externalId = preferencesManager.getExternalId().first()
            val profileId = preferencesManager.getCurrentProfileId()

            if (externalId != null && profileId != null) {
                _authState.value = AuthState.Authenticated(profileId)
            } else {
                // Token exists but missing profile info - need to refresh
                refreshUserProfile(token)
            }
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    // Auth with external provider token (Google, etc.)
    suspend fun authenticateWithIdpToken(idpToken: String, provider: String): Result<UUID> {
        try {
            val response = authApi.authenticateWithIdp(provider, idpToken)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                handleSuccessfulAuth(authResponse)
                return Result.success(authResponse.profileId)
            } else {
                _authState.value = AuthState.Error("Authentication failed: ${response.code()}")
                return Result.failure(Exception("Authentication failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            return Result.failure(e)
        }
    }

    private suspend fun handleSuccessfulAuth(authResponse: AuthResponse) {
        // Save auth token
        preferencesManager.setAuthToken(authResponse.token)
        preferencesManager.setExternalId(authResponse.externalId)
        preferencesManager.setCurrentProfileId(authResponse.profileId)

        // Save or update profile in local DB
        val profile = ProfileEntity(
            id = authResponse.profileId,
            externalId = authResponse.externalId,
            displayName = authResponse.displayName,
            email = authResponse.email,
            photoUrl = authResponse.photoUrl,
            isDefault = true
        )

        // Clear any existing default profiles
        profileDao.clearDefaultFlag()

        // Save this profile
        profileDao.insertProfile(profile)

        _authState.value = AuthState.Authenticated(authResponse.profileId)
    }

    // Refresh user profile from server
    suspend fun refreshUserProfile(token: String? = null): Result<UUID> {
        val authToken = token ?: preferencesManager.getAuthToken().first()
        if (authToken == null) {
            _authState.value = AuthState.NotAuthenticated
            return Result.failure(Exception("No auth token"))
        }

        try {
            val response = authApi.getUserProfile("Bearer $authToken")

            if (response.isSuccessful && response.body() != null) {
                val profileResponse = response.body()!!

                // Update local profile
                val profile = ProfileEntity(
                    id = profileResponse.profileId,
                    externalId = profileResponse.externalId,
                    displayName = profileResponse.displayName,
                    email = profileResponse.email,
                    photoUrl = profileResponse.photoUrl,
                    isDefault = true
                )

                profileDao.clearDefaultFlag()
                profileDao.insertProfile(profile)

                preferencesManager.setCurrentProfileId(profile.id)
                preferencesManager.setExternalId(profile.externalId)

                _authState.value = AuthState.Authenticated(profile.id)
                return Result.success(profile.id)
            } else {
                if (response.code() == 401) {
                    // Token expired
                    logout()
                    _authState.value = AuthState.TokenExpired
                } else {
                    _authState.value = AuthState.Error("Profile refresh failed: ${response.code()}")
                }
                return Result.failure(Exception("Profile refresh failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Profile refresh failed")
            return Result.failure(e)
        }
    }

    // Logout and clear data
    suspend fun logout() {
        preferencesManager.clearAllData()
        _authState.value = AuthState.NotAuthenticated
    }
}

sealed class AuthState {
    object NotAuthenticated : AuthState()
    data class Authenticated(val profileId: UUID) : AuthState()
    object TokenExpired : AuthState()
    data class Error(val message: String) : AuthState()
}package com.neyra.gymapp.data.auth

import com.neyra.gymapp.data.api.AuthApi
import com.neyra.gymapp.data.api.models.AuthResponse
import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.data.dao.ProfileDao
import com.neyra.gymapp.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val authApi: AuthApi,
    private val profileDao: ProfileDao,
    private val preferencesManager: PreferencesManager
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: Flow<AuthState> = _authState

    // Initialize auth state based on stored token
    suspend fun initialize() {
        val token = preferencesManager.getAuthToken().first()
        if (token != null) {
            val externalId = preferencesManager.getExternalId().first()
            val profileId = preferencesManager.getCurrentProfileId()

            if (externalId != null && profileId != null) {
                _authState.value = AuthState.Authenticated(profileId)
            } else {
                // Token exists but missing profile info - need to refresh
                refreshUserProfile(token)
            }
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    // Auth with external provider token (Google, etc.)
    suspend fun authenticateWithIdpToken(idpToken: String, provider: String): Result<UUID> {
        try {
            val response = authApi.authenticateWithIdp(provider, idpToken)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                handleSuccessfulAuth(authResponse)
                return Result.success(authResponse.profileId)
            } else {
                _authState.value = AuthState.Error("Authentication failed: ${response.code()}")
                return Result.failure(Exception("Authentication failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            return Result.failure(e)
        }
    }

    private suspend fun handleSuccessfulAuth(authResponse: AuthResponse) {
        // Save auth token
        preferencesManager.setAuthToken(authResponse.token)
        preferencesManager.setExternalId(authResponse.externalId)
        preferencesManager.setCurrentProfileId(authResponse.profileId)

        // Save or update profile in local DB
        val profile = ProfileEntity(
            id = authResponse.profileId,
            externalId = authResponse.externalId,
            displayName = authResponse.displayName,
            email = authResponse.email,
            photoUrl = authResponse.photoUrl,
            isDefault = true
        )

        // Clear any existing default profiles
        profileDao.clearDefaultFlag()

        // Save this profile
        profileDao.insertProfile(profile)

        _authState.value = AuthState.Authenticated(authResponse.profileId)
    }

    // Refresh user profile from server
    suspend fun refreshUserProfile(token: String? = null): Result<UUID> {
        val authToken = token ?: preferencesManager.getAuthToken().first()
        if (authToken == null) {
            _authState.value = AuthState.NotAuthenticated
            return Result.failure(Exception("No auth token"))
        }

        try {
            val response = authApi.getUserProfile("Bearer $authToken")

            if (response.isSuccessful && response.body() != null) {
                val profileResponse = response.body()!!

                // Update local profile
                val profile = ProfileEntity(
                    id = profileResponse.profileId,
                    externalId = profileResponse.externalId,
                    displayName = profileResponse.displayName,
                    email = profileResponse.email,
                    photoUrl = profileResponse.photoUrl,
                    isDefault = true
                )

                profileDao.clearDefaultFlag()
                profileDao.insertProfile(profile)

                preferencesManager.setCurrentProfileId(profile.id)
                preferencesManager.setExternalId(profile.externalId)

                _authState.value = AuthState.Authenticated(profile.id)
                return Result.success(profile.id)
            } else {
                if (response.code() == 401) {
                    // Token expired
                    logout()
                    _authState.value = AuthState.TokenExpired
                } else {
                    _authState.value = AuthState.Error("Profile refresh failed: ${response.code()}")
                }
                return Result.failure(Exception("Profile refresh failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Profile refresh failed")
            return Result.failure(e)
        }
    }

    // Logout and clear data
    suspend fun logout() {
        preferencesManager.clearAllData()
        _authState.value = AuthState.NotAuthenticated
    }
}

sealed class AuthState {
    object NotAuthenticated : AuthState()
    data class Authenticated(val profileId: UUID) : AuthState()
    object TokenExpired : AuthState()
    data class Error(val message: String) : AuthState()
}