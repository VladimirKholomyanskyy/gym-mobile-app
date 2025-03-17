package com.neyra.gymapp.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.neyra.gymapp.data.dao.ProfileDao
import com.neyra.gymapp.data.preferences.UserPreferences
import com.neyra.gymapp.data.repository.ProfileRepository
import com.neyra.gymapp.domain.ProfileManager
import com.neyra.gymapp.openapi.apis.ProfileApi
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
    fun provideProfileRepository(api: ProfileApi, dao: ProfileDao): ProfileRepository {
        return ProfileRepository(api, dao)
    }

    @Provides
    @Singleton
    fun provideProfileManager(
        userPreferences: UserPreferences,
        profileRepository: ProfileRepository
    ): ProfileManager {
        return ProfileManager(userPreferences, profileRepository)
    }
}