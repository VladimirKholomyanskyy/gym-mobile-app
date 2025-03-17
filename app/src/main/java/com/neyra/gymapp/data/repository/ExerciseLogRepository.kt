package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseLogDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.ExerciseLogsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseLogRepository @Inject constructor(
    private val exerciseLogDao: ExerciseLogDao,
    private val exerciseLogApi: ExerciseLogsApi,
    private val networkManager: NetworkManager
) {
}