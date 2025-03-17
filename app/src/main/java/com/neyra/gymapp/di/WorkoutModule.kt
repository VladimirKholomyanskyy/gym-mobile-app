package com.neyra.gymapp.di

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.repository.WorkoutRepositoryImpl
import com.neyra.gymapp.domain.repository.WorkoutRepository
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkoutModule {

    @Provides
    @Singleton
    fun provideWorkoutRepository(
        workoutDao: WorkoutDao,
        workoutExerciseDao: WorkoutExerciseDao,
        exerciseDao: ExerciseDao,
        workoutsApi: WorkoutsApi,
        workoutExercisesApi: WorkoutExercisesApi,
        networkManager: NetworkManager
    ): WorkoutRepository {
        return WorkoutRepositoryImpl(
            workoutDao = workoutDao,
            workoutExerciseDao = workoutExerciseDao,
            exerciseDao = exerciseDao,
            workoutsApi = workoutsApi,
            workoutExercisesApi = workoutExercisesApi,
            networkManager = networkManager
        )
    }
}