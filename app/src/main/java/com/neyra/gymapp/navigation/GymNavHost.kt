package com.neyra.gymapp.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.neyra.gymapp.openapi.models.TrainingProgram
import com.neyra.gymapp.openapi.models.WorkoutResponse
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
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


