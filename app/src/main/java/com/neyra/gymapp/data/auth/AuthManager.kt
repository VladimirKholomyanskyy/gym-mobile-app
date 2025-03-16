package com.neyra.gymapp.data.auth

import android.content.Context
import android.util.Log
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthSessionResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.neyra.gymapp.openapi.apis.AuthApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private var isInitialized = false

    suspend fun initialize() {
        if (isInitialized) return

        try {
            // Fetch auth config from API
            val response = authApi.getAuthConfig()
            if (response.isSuccessful && response.body() != null) {
                val config = response.body()!!
                // Configure Amplify with auth config
                val configJson = JSONObject().apply {
                    put("auth", JSONObject().apply {
                        put("plugins", JSONObject().apply {
                            put("awsCognitoAuthPlugin", JSONObject().apply {
                                put("UserAgent", "aws-amplify-android/2.14.9")
                                put("IdentityManager", JSONObject().apply {
                                    put("Default", JSONObject())
                                })
                                put("CognitoUserPool", JSONObject().apply {
                                    put("Default", JSONObject().apply {
                                        put("PoolId", config.userPoolId)
                                        put("AppClientId", config.userPoolClientId)
                                        put("Region", config.userPoolRegion)
                                    })
                                })
                                put("Auth", JSONObject().apply {
                                    put("Default", JSONObject().apply {
                                        put("authenticationFlowType", config.authenticationFlowType)
                                    })
                                })
                            })
                        })
                    })
                }

                val amplifyConfig = AmplifyConfiguration.fromJson(configJson)

                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(amplifyConfig, context)
                isInitialized = true

                // Check if user is already signed in
                checkAuthStatus()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing auth", e)
            _authState.value =
                AuthState.Error("Failed to initialize authentication: ${e.message}")
        }

    }

    private suspend fun checkAuthStatus() {
        try {
            val session = withContext(Dispatchers.IO) {
                getAuthSession()
            }

            if (session.isSignedIn) {
                // Get user ID from the Cognito session
                val userId = session.userSubId ?: ""
                _authState.value = AuthState.Authenticated(userId)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking auth status", e)
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signUp(
        username: String,
        password: String,
        email: String
    ): Result<AuthSignUpResult> {
        return try {

            val attributes = AuthUserAttribute(AuthUserAttributeKey.email(), email)
            val options = AuthSignUpOptions.builder()
                .userAttributes(listOf(attributes))
                .build()

            val result = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.signUp(
                        username,
                        password,
                        options,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun confirmSignUp(username: String, confirmationCode: String): Result<Boolean> {
        return try {
            val result = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.confirmSignUp(
                        username,
                        confirmationCode,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }
            }

            Result.success(result.isSignUpComplete)
        } catch (e: Exception) {
            Log.e(TAG, "Confirm sign up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(username: String, password: String): Result<AuthSignInResult> {
        return try {
            val result = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.signIn(
                        username,
                        password,
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }
            }

            if (result.isSignedIn) {
                checkAuthStatus()
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.signOut { result ->
                        when (result) {
                            is AWSCognitoAuthSignOutResult.CompleteSignOut -> {
                                continuation.resume(Result.success(true))
                            }

                            is AWSCognitoAuthSignOutResult.PartialSignOut -> {
                                // This occurs when sign out from some services succeeded, but not from others
                                Log.w(TAG, "Partial sign out: ${result.hostedUIError?.exception}")
                                continuation.resume(Result.success(true))
                            }

                            is AWSCognitoAuthSignOutResult.FailedSignOut -> {
                                continuation.resume(Result.failure(result.exception))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        } finally {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun getAuthToken(): String? {
        return try {
            val session = withContext(Dispatchers.IO) {
                getAuthSession()
            }
            session.accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get auth token", e)
            null
        }
    }

    private suspend fun getAuthSession(): CognitoAuthSession {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                Amplify.Auth.fetchAuthSession(
                    { continuation.resume(it.toCognitoSession()) },
                    { continuation.resumeWithException(it) }
                )
            }
        }
    }

    private fun AuthSession.toCognitoSession(): CognitoAuthSession {
        val session = this as AWSCognitoAuthSession

        return CognitoAuthSession(
            isSignedIn = session.isSignedIn,
            userSubId = when (session.identityIdResult.type) {
                AuthSessionResult.Type.SUCCESS -> session.identityIdResult.value
                else -> null
            },
            accessToken = when (session.userPoolTokensResult.type) {
                AuthSessionResult.Type.SUCCESS -> session.userPoolTokensResult.value?.idToken
                else -> null
            }
        )
    }

    data class CognitoAuthSession(
        val isSignedIn: Boolean,
        val userSubId: String?,
        val accessToken: String?
    )

    companion object {
        private const val TAG = "AuthManager"
    }
}