package com.neyra.gymapp.di

import android.content.Context
import com.neyra.gymapp.data.auth.AuthManager
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.network.NetworkManagerImpl
import com.neyra.gymapp.openapi.apis.AuthApi
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
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import javax.inject.Qualifier
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
    fun provideAuthInterceptor(authManager: AuthManager): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()

            // Skip auth for the auth config endpoint
            if (original.url.toString().contains("/auth/config")) {
                return@Interceptor chain.proceed(original)
            }

            // Get a valid token (refreshed if needed)
            val token = runBlocking { authManager.getValidAuthToken() }

            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .method(original.method, original.body)
                    .build()
            } else {
                // No valid token - proceed without auth header
                original
            }

            // Process the request
            val response = chain.proceed(request)

            // Handle 401 Unauthorized errors
            if (response.code == 401) {
                response.close()

                // Force token refresh and retry
                val newToken = runBlocking {
                    authManager.refreshToken()
                }

                return@Interceptor if (newToken != null) {
                    // Retry with new token
                    chain.proceed(
                        original.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .method(original.method, original.body)
                            .build()
                    )
                } else {
                    // Still no valid token, return the 401 response
                    response
                }
            }

            response
        }
    }

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedApiClient(authInterceptor: Interceptor): ApiClient {
        return ApiClient().addAuthorization(
            authName = "cognito",
            authorization = authInterceptor
        )
    }

    @Provides
    @Singleton
    @UnauthenticatedClient
    fun provideUnauthenticatedApiClient(): ApiClient {
        return ApiClient()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@UnauthenticatedClient apiClient: ApiClient): AuthApi {
        return ApiClient().createService(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProfilesApi(@AuthenticatedClient apiClient: ApiClient): ProfileApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTrainingProgramsApi(@AuthenticatedClient apiClient: ApiClient): TrainingProgramsApi {
        return apiClient.createService(TrainingProgramsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWorkoutsApi(@AuthenticatedClient apiClient: ApiClient): WorkoutsApi {
        return apiClient.createService(WorkoutsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWorkoutExercisesApi(@AuthenticatedClient apiClient: ApiClient): WorkoutExercisesApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(WorkoutExercisesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExercisesApi(@AuthenticatedClient apiClient: ApiClient): ExercisesApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(ExercisesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionsApi(@AuthenticatedClient apiClient: ApiClient): WorkoutSessionsApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(WorkoutSessionsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExerciseLogsApi(@AuthenticatedClient apiClient: ApiClient): ExerciseLogsApi {
        // Repeat for other APIs generated from OpenAPI.
        return apiClient.createService(ExerciseLogsApi::class.java)
    }


}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UnauthenticatedClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient