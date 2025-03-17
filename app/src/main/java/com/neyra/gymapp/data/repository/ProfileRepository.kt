package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ProfileDao
import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.data.mapper.toEntity
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.ProfileApi


import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileApi: ProfileApi,
    private val profileDao: ProfileDao,
    private val networkManager: NetworkManager
) {

    suspend fun getProfile(profileId: UUID): ProfileEntity? {
        val cachedProfile = profileDao.getProfile(profileId)
        return cachedProfile ?: profileApi.getProfile().body()?.toEntity().also {
            if (it != null) profileDao.insertProfile(it)
        }
    }
}