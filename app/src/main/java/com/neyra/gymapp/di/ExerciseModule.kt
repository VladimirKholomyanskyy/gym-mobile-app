package com.neyra.gymapp.di

import com.neyra.gymapp.data.dao.ExerciseDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.repository.ExerciseRepositoryImpl
import com.neyra.gymapp.domain.repository.ExerciseRepository
import com.neyra.gymapp.openapi.apis.ExercisesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExerciseModule {

    @Provides
    @Singleton
    fun provideExerciseRepository(
        exerciseDao: ExerciseDao,
        exercisesApi: ExercisesApi,
        networkManager: NetworkManager
    ): ExerciseRepository {
        return ExerciseRepositoryImpl(
            exerciseDao = exerciseDao,
            exercisesApi = exercisesApi,
            networkManager = networkManager
        )
    }
}