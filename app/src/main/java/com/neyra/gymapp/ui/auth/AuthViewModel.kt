package com.neyra.gymapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neyra.gymapp.data.auth.AuthManager
import com.neyra.gymapp.data.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = authManager.authState

    init {
        viewModelScope.launch {
            authManager.initialize()
        }
    }

    fun signIn(username: String, password: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authManager.signIn(username, password)

                if (result.isSuccess) {
                    callback(true, null)
                } else {
                    result.exceptionOrNull()?.let {
                        callback(false, it.message ?: "Sign in failed")
                    }
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Sign in failed")
            }
        }
    }

    fun signUp(
        username: String,
        password: String,
        email: String,
        callback: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = authManager.signUp(username, password, email)

                if (result.isSuccess) {
                    callback(true, null)
                } else {
                    result.exceptionOrNull()?.let {
                        callback(false, it.message ?: "Sign up failed")
                    }
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Sign up failed")
            }
        }
    }

    fun confirmSignUp(
        username: String,
        confirmationCode: String,
        callback: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = authManager.confirmSignUp(username, confirmationCode)

                if (result.isSuccess && result.getOrNull() == true) {
                    callback(true, null)
                } else {
                    result.exceptionOrNull()?.let {
                        callback(false, it.message ?: "Confirmation failed")
                    } ?: callback(false, "Confirmation failed")
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Confirmation failed")
            }
        }
    }

    fun signOut(callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = authManager.signOut()

                if (result.isSuccess) {
                    callback(true, null)
                } else {
                    result.exceptionOrNull()?.let {
                        callback(false, it.message ?: "Sign out failed")
                    }
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Sign out failed")
            }
        }
    }
}