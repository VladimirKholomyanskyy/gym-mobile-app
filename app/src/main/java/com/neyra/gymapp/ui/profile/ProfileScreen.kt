package com.neyra.gymapp.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.ui.auth.AuthViewModel
import com.neyra.gymapp.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { showLogoutConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Your profile content here

            // Logout confirmation dialog
            if (showLogoutConfirmation) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirmation = false },
                    title = { Text("Logout") },
                    text = { Text("Are you sure you want to logout?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                isLoggingOut = true
                                authViewModel.signOut { success, message ->
                                    isLoggingOut = false
                                    showLogoutConfirmation = false
                                    // Auth state flow will handle navigation
                                }
                            },
                            enabled = !isLoggingOut
                        ) {
                            if (isLoggingOut) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Logout")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}