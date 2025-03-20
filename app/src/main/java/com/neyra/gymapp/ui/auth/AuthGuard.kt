package com.neyra.gymapp.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.data.auth.AuthState
import com.neyra.gymapp.ui.components.LoadingScreen

@Composable
fun AuthGuard(
    authViewModel: AuthViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.Authenticated -> content()
        is AuthState.Loading -> LoadingScreen()
        else -> {
            // This should generally not happen due to the LaunchedEffect in GymNavHost,
            // but it's good to have a fallback
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Please log in to continue")
            }
        }
    }
}