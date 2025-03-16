package com.neyra.gymapp.di

import android.content.Context
import androidx.room.Room
import com.neyra.gymapp.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    fun provideTrainingProgramDao(database: AppDatabase) = database.trainingProgramDao()

    @Provides
    fun provideWorkoutDao(database: AppDatabase) = database.workoutDao()

    @Provides
    fun provideExerciseDao(database: AppDatabase) = database.exerciseDao()


}