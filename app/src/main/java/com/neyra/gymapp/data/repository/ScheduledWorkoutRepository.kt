package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ScheduledWorkoutDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.ScheduledWorkoutsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledWorkoutRepository @Inject constructor(
    private val scheduledWorkoutDao: ScheduledWorkoutDao,
    private val scheduledWorkoutApi: ScheduledWorkoutsApi,
    private val networkManager: NetworkManager
) {
}