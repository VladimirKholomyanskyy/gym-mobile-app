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
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTrainingProgramDao(database: AppDatabase) = database.trainingProgramDao()

    @Provides
    fun provideWorkoutDao(database: AppDatabase) = database.workoutDao()

    @Provides
    fun provideWorkoutExerciseDao(database: AppDatabase) = database.workoutExerciseDao()

    @Provides
    fun provideWorkoutSessionDao(database: AppDatabase) = database.workoutSessionDao()

    @Provides
    fun provideExerciseLogDao(database: AppDatabase) = database.exerciseLogDao()

    @Provides
    fun provideScheduledWorkoutDao(database: AppDatabase) = database.scheduledWorkoutDao()

    @Provides
    fun provideProfileDao(database: AppDatabase) = database.profileDao()

    @Provides
    fun provideSettingsDao(database: AppDatabase) = database.settingsDao()

    @Provides
    fun provideExerciseDao(database: AppDatabase) = database.exerciseDao()


}