package com.neyra.gymapp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.neyra.gymapp.data.dao.TrainingProgramDao
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.preferences.UserPreferences
import com.neyra.gymapp.data.repository.TrainingProgramRepositoryImpl
import com.neyra.gymapp.domain.repository.TrainingProgramRepository
import com.neyra.gymapp.openapi.apis.TrainingProgramsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("user_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideTrainingProgramRepository(
        trainingProgramDao: TrainingProgramDao,
        trainingProgramsApi: TrainingProgramsApi,
        networkManager: NetworkManager
    ): TrainingProgramRepository {
        return TrainingProgramRepositoryImpl(
            trainingProgramDao,
            trainingProgramsApi,
            networkManager
        )
    }


}