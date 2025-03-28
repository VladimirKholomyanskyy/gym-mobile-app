package com.neyra.gymapp.di

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.dao.WorkoutExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.repository.WorkoutExerciseRepositoryImpl
import com.neyra.gymapp.domain.repository.WorkoutExerciseRepository
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkoutExerciseModule {

    @Provides
    @Singleton
    fun provideWorkoutExerciseRepository(
        workoutExerciseDao: WorkoutExerciseDao,
        exerciseDao: ExerciseDao,
        workoutExercisesApi: WorkoutExercisesApi,
        networkManager: NetworkManager
    ): WorkoutExerciseRepository {
        return WorkoutExerciseRepositoryImpl(
            workoutExerciseDao = workoutExerciseDao,
            exerciseDao = exerciseDao,
            workoutExerciseApi = workoutExercisesApi,
            networkManager = networkManager
        )
    }
}