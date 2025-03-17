package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.ExercisesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseApi: ExercisesApi,
    private val networkManager: NetworkManager
) {
}