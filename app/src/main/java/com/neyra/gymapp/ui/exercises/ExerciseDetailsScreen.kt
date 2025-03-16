package com.neyra.gymapp.ui.exercises

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.viewmodel.ExerciseDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailsScreen(
    exerciseId: String,
    viewModel: ExerciseDetailsViewModel = hiltViewModel()
) {
    val exerciseState by viewModel.exercise.collectAsState()
    val logsState by viewModel.exerciseLogs.collectAsState()
    val progressState by viewModel.exerciseProgress.collectAsState()

    // Fetch exercise details and logs when the screen appears.
    LaunchedEffect(exerciseId) {
        viewModel.fetchExercise(exerciseId)
        viewModel.fetchExerciseLogs(exerciseId)
        viewModel.fetchExerciseProgress(exerciseId)

    }

    // State for managing the selected tab.
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Description", "Logs", "Progress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (exerciseState) {
                        is UiState.Success -> Text((exerciseState as UiState.Success).data.name)
                        else -> Text("Exercise Details")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (exerciseState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> {
                    Text(
                        text = (exerciseState as UiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Success -> {
                    val exercise = (exerciseState as UiState.Success).data
                    Column {
                        // Create the TabRow with three tabs.
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                        // Display content based on the selected tab.
                        when (selectedTabIndex) {
                            0 -> {
                                // Description Tab
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Primary Muscle: ${exercise.primaryMuscle}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Secondary Muscles: ${exercise.secondaryMuscle?.joinToString() ?: "None"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Equipment: ${exercise.equipment ?: "None"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Description: ${exercise.description ?: "No description"}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            1 -> {
                                // Logs Tab
                                when (logsState) {
                                    is UiState.Loading -> {
                                        CircularProgressIndicator()
                                    }

                                    is UiState.Error -> {
                                        Text(
                                            text = (logsState as UiState.Error).message,
                                            color = Color.Red,
                                        )
                                    }

                                    is UiState.Success -> {
                                        val logs = (logsState as UiState.Success).data
                                        if (logs.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "No logs available",
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                        } else {
                                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                                items(logs) { log ->
                                                    // Customize how each log is displayed.
                                                    Text(
                                                        text = log.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Progress Tab (placeholder for now)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Graph coming soon",
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
