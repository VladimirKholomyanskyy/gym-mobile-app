package com.neyra.gymapp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.neyra.gymapp.data.auth.AuthState
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.network.NetworkManagerImpl
import com.neyra.gymapp.openapi.models.TrainingProgram
import com.neyra.gymapp.openapi.models.WorkoutResponse
import com.neyra.gymapp.ui.auth.AuthViewModel
import com.neyra.gymapp.ui.auth.ConfirmSignUpScreen
import com.neyra.gymapp.ui.auth.ForgotPasswordScreen
import com.neyra.gymapp.ui.auth.LoginScreen
import com.neyra.gymapp.ui.auth.ResetPasswordConfirmationScreen
import com.neyra.gymapp.ui.auth.SignUpScreen
import com.neyra.gymapp.ui.components.BottomNavBar
import com.neyra.gymapp.ui.exercises.ExerciseDetailsScreen
import com.neyra.gymapp.ui.exercises.ExerciseListScreen
import com.neyra.gymapp.ui.home.HomeScreen
import com.neyra.gymapp.ui.profile.ProfileScreen
import com.neyra.gymapp.ui.programs.TrainingScreen
import com.neyra.gymapp.ui.progress.WorkoutSessionScreen
import com.neyra.gymapp.ui.scheduling.CalendarView
import com.neyra.gymapp.ui.workouts.WorkoutExercisesScreen
import com.neyra.gymapp.ui.workouts.WorkoutsScreen

@Composable
fun GymNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    val offlineMode by authViewModel.offlineMode.collectAsState()
    val networkManager: NetworkManager = remember { NetworkManagerImpl(LocalContext.current) }
    // Determine start destination based on auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Only navigate if not already on a main screen
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == null || currentRoute.startsWith("auth_")) {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            is AuthState.Unauthenticated -> {
                // Only navigate if not already on an auth screen
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == null || !currentRoute.startsWith("auth_")) {
                    navController.navigate("auth_login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            // For Loading and Error states, we don't navigate automatically
            else -> {}
        }
    }
// Show an offline banner if in offline mode
    if (offlineMode) {
        OfflineBanner(
            isNetworkAvailable = networkManager.isOnline(),
            onReconnect = {
                authViewModel.reconnect()
            }
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { // Only show bottom nav when authenticated and not in an auth screen
            if (authState is AuthState.Authenticated) {
                BottomNavBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController,
            startDestination = "auth_login",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Auth navigation graph
            navigation(
                startDestination = "login",
                route = "auth"
            ) {
                composable("login") {
                    LoginScreen(
                        onNavigateToSignUp = {
                            navController.navigate("auth/signup")
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate("auth/forgot_password")
                        }
                    )
                }

                composable("signup") {
                    SignUpScreen(
                        onNavigateToLogin = {
                            navController.navigate("auth/login") {
                                popUpTo("auth") { inclusive = false }
                            }
                        },
                        onNavigateToConfirmation = { username ->
                            navController.navigate("auth/confirm_signup/$username")
                        }
                    )
                }

                composable(
                    "confirm_signup/{username}",
                    arguments = listOf(navArgument("username") { type = NavType.StringType })
                ) { backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username") ?: ""
                    ConfirmSignUpScreen(
                        username = username,
                        onConfirmationSuccess = {
                            navController.navigate("auth/login") {
                                popUpTo("auth") { inclusive = false }
                            }
                        }
                    )
                }

                composable("forgot_password") {
                    ForgotPasswordScreen(
                        onNavigateToLogin = {
                            navController.navigate("auth/login") {
                                popUpTo("auth") { inclusive = false }
                            }
                        },
                        onNavigateToResetConfirmation = { username ->
                            navController.navigate("auth/reset_password/$username")
                        }
                    )
                }

                composable(
                    "reset_password/{username}",
                    arguments = listOf(navArgument("username") { type = NavType.StringType })
                ) { backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username") ?: ""
                    ResetPasswordConfirmationScreen(
                        username = username,
                        onNavigateToLogin = {
                            navController.navigate("auth/login") {
                                popUpTo("auth") { inclusive = false }
                            }
                        }
                    )
                }
            }

            composable("home") { HomeScreen() }
            composable("calendar") { CalendarView() }
            composable("trainingPrograms") {
                TrainingScreen(
                    onTrainingSelected = { program ->
                        val programJson = Gson().toJson(program)
                        navController.navigate("trainingPrograms/$programJson")
                    },
                    onCalendarClicked = { navController.navigate("calendar") }
                )
            }
            composable(
                route = "trainingPrograms/{program}",
                arguments = listOf(navArgument("program") { type = NavType.StringType })
            ) { backStackEntry ->
                val programString = backStackEntry.arguments?.getString("program") ?: ""
                val program = Gson().fromJson(programString, TrainingProgram::class.java)
                WorkoutsScreen(
                    program = program,
                    onWorkoutSelected = { workout ->
                        val workoutJson = Gson().toJson(workout)
                        navController.navigate("workout-exercises/$workoutJson")
                    }
                )
            }
            composable(
                route = "workout-exercises/{workout}",
                arguments = listOf(navArgument("workout") { type = NavType.StringType })
            ) { backStackEntry ->
                val workoutJson = backStackEntry.arguments?.getString("workout") ?: ""
                val workout = Gson().fromJson(workoutJson, WorkoutResponse::class.java)
                WorkoutExercisesScreen(
                    workout = workout,
                    onWorkoutSelected = { exerciseId ->
                        navController.navigate("exercises/$exerciseId")
                    },
                    onBackPressed = { navController.navigateUp() },
                    onStartWorkoutSession = { sessionId -> navController.navigate("workout-sessions/$sessionId") }// Handle back navigation
                )
            }
            composable(
                route = "workout-sessions/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                WorkoutSessionScreen(
                    sessionId = sessionId,
                    onBackPressed = { navController.navigateUp() }
                )
            }
            composable("profile") { ProfileScreen() }
            composable("exercises") {
                ExerciseListScreen(navController)
            }
            composable("exercise_details/{exerciseId}") { backStackEntry ->
                val exerciseId =
                    backStackEntry.arguments?.getString("exerciseId") ?: return@composable
                ExerciseDetailsScreen(exerciseId)
            }
        }
    }
}

@Composable
fun OfflineBanner(
    isNetworkAvailable: Boolean,
    onReconnect: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offline Mode",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (isNetworkAvailable) {
                Button(
                    onClick = onReconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("Reconnect")
                }
            }
        }
    }
}

