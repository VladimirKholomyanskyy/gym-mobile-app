package com.neyra.gymapp.di

import android.content.Context
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.network.NetworkManagerImpl
import com.neyra.gymapp.openapi.apis.ExerciseLogsApi
import com.neyra.gymapp.openapi.apis.ExercisesApi
import com.neyra.gymapp.openapi.apis.ProfileApi
import com.neyra.gymapp.openapi.apis.TrainingProgramsApi
import com.neyra.gymapp.openapi.apis.WorkoutExercisesApi
import com.neyra.gymapp.openapi.apis.WorkoutSessionsApi
import com.neyra.gymapp.openapi.apis.WorkoutsApi
import com.neyra.gymapp.openapi.infrastructure.ApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkManager(
        @ApplicationContext context: Context
    ): NetworkManager {
        return NetworkManagerImpl(context)
    }

    @Provides
    @Singleton
    fun apiClient(): ApiClient {
        return ApiClient().setBearerToken(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIzcmFZb3NPYncwNkpJc0pZN194VUM1QUZHLUtYRUQybHViSEljdVRqQU9RIn0.eyJleHAiOjE3Mzg5MjkyMzUsImlhdCI6MTczODkxODQzNSwianRpIjoiNWUzMzY1MjQtM2Q5ZC00M2M2LThhNGItMjIyNGVjNzE1ZGI2IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDcwL3JlYWxtcy9nYWlueiIsImF1ZCI6WyJneW0tYXBpIiwiYWNjb3VudCJdLCJzdWIiOiJhNWNlMTJiMi0zZDRkLTQzOWMtYWM4ZC1jZDVjYTVkOGVhMzMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJyZWFjdC1jbGllbnQiLCJzaWQiOiIxYjJhYmMxYS02ZTVjLTQwMTgtODUzZC1kMjQxM2RiNzdjOTYiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3Q6NTE3MyJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLWdhaW56IiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6ImdhaW5lciBnb29kIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiZ2FpbmVyIiwiZ2l2ZW5fbmFtZSI6ImdhaW5lciIsImZhbWlseV9uYW1lIjoiZ29vZCIsImVtYWlsIjoiZ2FpbmVyQGdtYWlsLmNvbSJ9.Tm69v0tnvJNO5ctEhZZ8N3EW6R-TP9grzDj6KpA1H_1xl-OfiOLPJDCL0Xs28PloHUVBLRoJaPJbl5iXxY-xj4HdI3B2VUJ-uR0nqMPAUAtmosju-hzJLJ10sUUa4crlYH0Sp0BRf8nr_Xh5tX3vcSdS7uLGjR_p7hNaaauhRc6Keauog6xFCDxKhTRj2pRXrJrMX_1sTZyD-MRgXY8Fozb0NcSMmQqk9hWpe9pauh6d7v09V1dMHg3DQUvWWJrUKQ5XMx7-86oucX2gidpZHKBu6c3SqQg8wC_BJGdf1IqZ3nSZ3_58fZ-R8h7Ho5A8BI3aBrgm3J1bmIRBZjyG_w"
        )
    }

    @Provides
    @Singleton
    fun profilesApi(apiClient: ApiClient): ProfileApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun trainingProgramsApi(apiClient: ApiClient): TrainingProgramsApi {
        return apiClient.createService(TrainingProgramsApi::class.java)
    }

    @Provides
    @Singleton
    fun workoutsApi(apiClient: ApiClient): WorkoutsApi {
        return apiClient.createService(WorkoutsApi::class.java)
    }

    @Provides
    @Singleton
    fun workoutExercisesApi(apiClient: ApiClient): WorkoutExercisesApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(WorkoutExercisesApi::class.java)
    }

    @Provides
    @Singleton
    fun exercisesApi(apiClient: ApiClient): ExercisesApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(ExercisesApi::class.java)
    }

    @Provides
    @Singleton
    fun workoutSessionsApi(apiClient: ApiClient): WorkoutSessionsApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(WorkoutSessionsApi::class.java)
    }

    @Provides
    @Singleton
    fun exerciseLogsApi(apiClient: ApiClient): ExerciseLogsApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(ExerciseLogsApi::class.java)
    }


}