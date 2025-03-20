package com.neyra.gymapp.navigation

import android.util.Log
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
import com.neyra.gymapp.data.auth.AuthState
import com.neyra.gymapp.data.network.NetworkManager
import com.neyra.gymapp.data.network.NetworkManagerImpl
import com.neyra.gymapp.ui.auth.AuthGuard
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
import com.neyra.gymapp.ui.programs.TrainingProgramsScreen
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
    val context = LocalContext.current
    val networkManager: NetworkManager = remember(context) { NetworkManagerImpl(context) }

    // Debug the routes
    LaunchedEffect(Unit) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("Navigation", "Current destination: ${destination.route}")
        }
    }

    // Determine start destination based on auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Only navigate if not already on a main screen
                val currentRoute = navController.currentDestination?.route

                // Check if we're on an auth screen and navigate to main if needed

                navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                }

            }

            is AuthState.Unauthenticated -> {
                // Only navigate if not already on an auth screen
                Log.d("GymNavHost", "Unauthenticated")
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == null || !currentRoute.startsWith("auth")) {
                    navController.navigate("auth/login") {
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
            startDestination = "auth",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Auth navigation graph
            navigation(
                route = "auth",
                startDestination = "login"

            ) {
                composable("login") {
                    LoginScreen(
                        onNavigateToSignUp = {
                            navController.navigate("signup")
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate("forgot_password")
                        }
                    )
                }

                composable("signup") {
                    SignUpScreen(
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("auth") { inclusive = true }
                            }
                        },
                        onNavigateToConfirmation = { username ->
                            navController.navigate("confirm_signup/$username")
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
                            navController.navigate("login") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    )
                }

                composable("forgot_password") {
                    ForgotPasswordScreen(
                        onNavigateToLogin = {
                            navController.navigate("login") {
                                popUpTo("auth") { inclusive = true }
                            }
                        },
                        onNavigateToResetConfirmation = { username ->
                            navController.navigate("reset_password/$username")
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
                            navController.navigate("login") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Main app navigation graph
            navigation(
                startDestination = "home",
                route = "main"
            ) {
                composable("home") {
                    AuthGuard { HomeScreen() }
                }

                composable("calendar") {
                    AuthGuard { CalendarView() }
                }

                composable("trainingPrograms") {
                    AuthGuard {
                        TrainingProgramsScreen(
                            onProgramSelected = { trainingProgramId ->
                                navController.navigate("trainingPrograms/$trainingProgramId")
                            },
                            onCalendarClicked = { navController.navigate("calendar") }
                        )
                    }
                }

                composable(
                    route = "trainingPrograms/{trainingProgramId}",
                    arguments = listOf(navArgument("trainingProgramId") {
                        type = NavType.StringType
                    })
                ) { backStackEntry ->
                    val trainingProgramId =
                        backStackEntry.arguments?.getString("trainingProgramId") ?: ""
                    AuthGuard {
                        WorkoutsScreen(
                            trainingProgramId = trainingProgramId,
                            onWorkoutSelected = { workoutId ->
                                navController.navigate("workout-exercises/$workoutId")
                            },
                            onBackPressed = { navController.navigateUp() },
                        )
                    }
                }

                composable(
                    route = "workout-exercises/{workoutId}",
                    arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                    AuthGuard {
                        WorkoutExercisesScreen(
                            workoutId = workoutId,
                            onWorkoutSelected = { exerciseId ->
                                navController.navigate("exercises/$exerciseId")
                            },
                            onBackPressed = { navController.navigateUp() },
                            onStartWorkoutSession = { sessionId ->
                                navController.navigate("workout-sessions/$sessionId")
                            }
                        )
                    }
                }

                composable(
                    route = "workout-sessions/{sessionId}",
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                    AuthGuard {
                        WorkoutSessionScreen(
                            sessionId = sessionId,
                            onBackPressed = { navController.navigateUp() }
                        )
                    }
                }

                composable("profile") {
                    AuthGuard { ProfileScreen() }
                }

                composable("exercises") {
                    AuthGuard {
                        ExerciseListScreen(navController = navController)
                    }
                }

                composable(
                    "exercise_details/{exerciseId}",
                    arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val exerciseId =
                        backStackEntry.arguments?.getString("exerciseId") ?: return@composable
                    AuthGuard {
                        ExerciseDetailsScreen(exerciseId)
                    }
                }
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

