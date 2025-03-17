package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val workoutApi: WorkoutsApi,
    private val networkManager: NetworkManager
) {
}