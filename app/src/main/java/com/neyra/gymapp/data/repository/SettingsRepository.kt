package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.SettingsDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.SettingsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao,
    private val settingsApi: SettingsApi,
    private val networkManager: NetworkManager
) {
}