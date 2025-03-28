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
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.openapi.apis.AuthApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
    private val secureStorage: SecureStorage,
    private val networkManager: NetworkManager
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private var isInitialized = false

    // Token validity
    private var tokenExpirationTime: Long = 0
    private val tokenRefreshThreshold = 5 * 60 * 1000 // 5 minutes in milliseconds

    // Add offline authentication support
    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode

    suspend fun initialize() {
        if (isInitialized) return

        try {
            if (!networkManager.isOnline()) {
                // We're offline - check for cached credentials
                handleOfflineInitialization()
                return
            }
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
            Log.e(TAG, "Error fetching auth config", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing auth", e)
            handleOfflineInitialization()
        }

    }

    private suspend fun handleOfflineInitialization() {
        // Check if we have a stored session
        val storedToken = secureStorage.getToken()
        val userId = secureStorage.getUserId()

        if (storedToken != null && userId != null) {
            // We have stored credentials, enable offline mode
            _offlineMode.value = true
            _authState.value = AuthState.Authenticated(userId)
            Log.i(TAG, "Enabling offline mode with cached credentials")
        } else {
            // No stored credentials, force login
            _authState.value = AuthState.Unauthenticated
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
        // If we're offline and not allowing offline auth, fail immediately
        if (!networkManager.isOnline()) {
            return Result.failure(Exception("Cannot sign in while offline"))
        }

        try {
            // First attempt to sign in
            return trySignIn(username, password)
        } catch (e: Exception) {
            // Check if this is the "already signed in" error
            if (e.message?.contains("SignedInException") == true ||
                e.toString().contains("SignedInException")
            ) {

                Timber.d("User already signed in, attempting to sign out and retry")

                // Try to sign out and retry sign in
                try {
                    // Force sign out
                    withContext(Dispatchers.IO) {
                        suspendCancellableCoroutine<Unit> { continuation ->
                            Amplify.Auth.signOut {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    // Clear local storage
                    secureStorage.clearAll()

                    // Wait a brief moment to ensure sign out completes
                    delay(500)

                    // Try sign in again
                    return trySignIn(username, password)
                } catch (retryEx: Exception) {
                    Timber.e(retryEx, "Failed to retry sign in after sign out")

                    val errorMessage =
                        "Failed to sign in after signing out previous user: ${retryEx.message}"
                    _authState.value = AuthState.Error(errorMessage)
                    return Result.failure(Exception(errorMessage))
                }
            } else {
                // Handle other errors as before
                val errorMessage = processAuthError(e)
                _authState.value = AuthState.Error(errorMessage)
                return Result.failure(Exception(errorMessage))
            }
        }
    }

    // Helper method to attempt sign in and process successful result
    private suspend fun trySignIn(username: String, password: String): Result<AuthSignInResult> {
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
            val session = getAuthSession()
            val userId = session.userSubId ?: ""

            // Save credentials for offline use
            val token = session.accessToken
            if (token != null && userId.isNotEmpty()) {
                secureStorage.saveToken(token)
                secureStorage.saveUserId(userId)
            }

            tokenExpirationTime = System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD
            _authState.value = AuthState.Authenticated(userId)
            _offlineMode.value = false  // We're online now
        }

        return Result.success(result)
    }

    // Helper method to process auth errors into user-friendly messages
    private fun processAuthError(e: Exception): String {
        return when {
            e.message?.contains("UserNotConfirmedException") == true ->
                "User is not confirmed. Please check your email for confirmation code."

            e.message?.contains("NotAuthorizedException") == true ->
                "Incorrect username or password"

            e.message?.contains("UserNotFoundException") == true ->
                "User does not exist"

            e.message?.contains("network") == true ||
                    e.message?.contains("connect") == true ->
                "Network error. Please check your connection."

            else -> e.message ?: "Authentication failed"
        }
    }

    suspend fun signOut(): Result<Boolean> {
        // Clear local storage regardless of online/offline
        secureStorage.clearAll()

        // If offline, just update the state
        if (_offlineMode.value) {
            _authState.value = AuthState.Unauthenticated
            return Result.success(true)
        }
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
            // If network error during signout, still clear local state
            if (!networkManager.isOnline()) {
                _authState.value = AuthState.Unauthenticated
                return Result.success(true)
            }

            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        } finally {
            _offlineMode.value = false
        }
    }

    // Method to try reconnecting when network becomes available
    suspend fun reconnect() {
        if (!_offlineMode.value || !networkManager.isOnline()) {
            return
        }

        try {
            // Try to validate the session with the server
            val session = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.fetchAuthSession(
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }
            }

            if (session.isSignedIn) {
                // Session is still valid, switch to online mode
                _offlineMode.value = false

                // Update stored tokens with fresh ones
                val cognitoSession = session as AWSCognitoAuthSession
                if (cognitoSession.userPoolTokensResult.type == AuthSessionResult.Type.SUCCESS) {
                    val tokens = cognitoSession.userPoolTokensResult.value
                    if (tokens != null) {
                        tokens.idToken?.let { secureStorage.saveToken(it) }
                        tokenExpirationTime = System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD
                    }
                }
            } else {
                // Session is invalid, force re-login
                _authState.value = AuthState.Unauthenticated
                secureStorage.clearAll()
            }
        } catch (e: Exception) {
            // Keep offline mode active on errors
            Log.e(TAG, "Failed to reconnect", e)
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

    // Handle token refreshing
    suspend fun getValidAuthToken(): String? {
        // If we're in offline mode, return cached token without validation
        if (_offlineMode.value) {
            return secureStorage.getToken()
        }
        // First try to get from secure storage
        val storedToken = secureStorage.getToken()
        val tokenExpiry = secureStorage.getTokenExpiry()
        val currentTime = System.currentTimeMillis()

        // If token is valid and not about to expire, use it
        if (storedToken != null && currentTime < tokenExpiry - tokenRefreshThreshold) {
            return storedToken
        }

        // Otherwise, refresh the token
        return refreshToken()
    }

    suspend fun refreshToken(): String? {
        try {
            val session = getAuthSession()

            if (!session.isSignedIn) {
                _authState.value = AuthState.Unauthenticated
                return null
            }

            // Get fresh tokens
            val freshToken = session.accessToken

            // Update expiration time - typically 1 hour for Cognito tokens
            tokenExpirationTime = System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD

            return freshToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token", e)
            _authState.value = AuthState.Unauthenticated
            return null
        }
    }

    suspend fun resetPassword(username: String): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.resetPassword(
                        username,
                        { continuation.resume(Result.success(true)) },
                        { continuation.resume(Result.failure(it)) }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reset password failed", e)
            Result.failure(e)
        }
    }

    suspend fun confirmResetPassword(
        username: String,
        newPassword: String,
        confirmationCode: String
    ): Result<Boolean> {
        return try {
            withContext(Dispatchers.IO) {
                suspendCancellableCoroutine { continuation ->
                    Amplify.Auth.confirmResetPassword(
                        username,
                        newPassword,
                        confirmationCode,
                        { continuation.resume(Result.success(true)) },
                        { continuation.resume(Result.failure(it)) }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Confirm reset password failed", e)
            Result.failure(e)
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
        private const val TOKEN_VALIDITY_PERIOD = 60 * 60 * 1000L // 1 hour in milliseconds
    }
}