package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.WorkoutSessionDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.WorkoutSessionsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSessionRepository @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSessionApi: WorkoutSessionsApi,
    private val networkManager: NetworkManager
) {
}