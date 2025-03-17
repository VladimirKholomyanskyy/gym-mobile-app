package com.neyra.gymapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val PROFILE_ID_KEY = stringPreferencesKey("profile_id")
    }

    /** Flow that emits the stored profile ID as UUID (nullable if not set) */
    val profileIdFlow: Flow<String?> = dataStore.data
        .map { preferences -> preferences[PROFILE_ID_KEY] }

    /** Fetch the profile ID as a suspend function (returns null if not found) */
    suspend fun getProfileId(): String? {
        return profileIdFlow.firstOrNull()
    }

    /** Save the profile ID as UUID */
    suspend fun saveProfileId(profileId: String) {
        dataStore.edit { preferences ->
            preferences[PROFILE_ID_KEY] = profileId.toString() // Convert UUID to String
        }
    }

    /** Clear the profile ID (e.g., on logout) */
    suspend fun clearProfileId() {
        dataStore.edit { preferences ->
            preferences.remove(PROFILE_ID_KEY)
        }
    }
}