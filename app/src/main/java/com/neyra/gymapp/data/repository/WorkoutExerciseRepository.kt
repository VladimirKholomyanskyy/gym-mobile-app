package com.neyra.gymapp.data.repository

import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutExerciseRepository @Inject constructor(
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val workoutExerciseApi: WorkoutExercisesApi,
    private val networkManager: NetworkManager
) {
}