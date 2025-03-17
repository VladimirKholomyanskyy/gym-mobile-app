package com.neyra.gymapp.ui.workouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.viewmodel.WorkoutsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    trainingProgramId: String,
    onWorkoutSelected: (workoutId: String) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel()
) {
    // Trigger fetching of workouts when the screen appears or programId changes
    LaunchedEffect(trainingProgramId) {
        viewModel.fetchWorkouts(trainingProgramId)
    }

    val workoutsState by viewModel.workouts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isCreateWorkoutDrawerVisible by viewModel.isCreateWorkoutDrawerVisible.collectAsState()
    val isUpdateWorkoutDrawerVisible by viewModel.isUpdateWorkoutDrawerVisible.collectAsState()
    val selectedWorkout by viewModel.selectedWorkout.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in snackbar if present
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Workouts") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateWorkoutDrawer() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Workout")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = workoutsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No workouts yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add your first workout to get started",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.showCreateWorkoutDrawer() }) {
                                Text("Add Workout")
                            }
                        }
                    } else {
                        // List of workouts
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            items(state.data) { workout ->
                                WorkoutCard(
                                    workout = workout,
                                    onClick = { onWorkoutSelected(workout.id ?: "") },
                                    onEdit = {
                                        viewModel.setSelectedWorkout(workout)
                                        viewModel.showUpdateWorkoutDrawer()
                                    },
                                    onDelete = {
                                        workout.id?.let { id ->
                                            viewModel.deleteWorkout(trainingProgramId, id)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Something went wrong",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.fetchWorkouts(trainingProgramId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        // Create Workout Drawer
        if (isCreateWorkoutDrawerVisible) {
            CreateWorkoutDrawer(
                onCancel = { viewModel.hideCreateWorkoutDrawer() },
                onCreate = { name -> viewModel.createWorkout(trainingProgramId, name) }
            )
        }

        // Update Workout Drawer
        if (isUpdateWorkoutDrawerVisible && selectedWorkout != null) {
            UpdateWorkoutDrawer(
                workout = selectedWorkout!!,
                onCancel = { viewModel.hideUpdateWorkoutDrawer() },
                onUpdate = { updatedName ->
                    selectedWorkout?.id?.let { id ->
                        viewModel.updateWorkout(trainingProgramId, id, updatedName)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isDrawerVisible by remember { mutableStateOf(false) }
    var isConfirmationVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Workout Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleLarge
                )

                // Show exercise count if available
                if (workout.exercises.isNotEmpty()) {
                    Text(
                        text = "${workout.exercises.size} exercises",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Show estimated duration if available
                val duration = workout.estimateDuration()
                if (duration.toMinutes() > 0) {
                    Text(
                        text = "Est. time: ${duration.toMinutes()} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Menu Button with 3 Dots
            IconButton(onClick = { isDrawerVisible = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }
        }
    }

    // Options Drawer
    if (isDrawerVisible) {
        ModalBottomSheet(
            onDismissRequest = { isDrawerVisible = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                HorizontalDivider()
                TextButton(
                    onClick = {
                        isDrawerVisible = false
                        onEdit()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit", style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(
                    onClick = {
                        isDrawerVisible = false
                        isConfirmationVisible = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Delete Confirmation
    if (isConfirmationVisible) {
        ConfirmationBottomDrawer(
            message = "Are you sure you want to delete this workout?",
            onConfirm = {
                onDelete()
                isConfirmationVisible = false
            },
            onCancel = { isConfirmationVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWorkoutDrawer(
    onCancel: () -> Unit,
    onCreate: (String) -> Unit
) {
    var workoutName by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Create Workout",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = workoutName,
                onValueChange = {
                    workoutName = it
                    isNameError = it.isBlank()
                },
                label = { Text("Workout Name") },
                isError = isNameError,
                supportingText = {
                    if (isNameError) {
                        Text("Name cannot be empty")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (workoutName.isBlank()) {
                            isNameError = true
                        } else {
                            onCreate(workoutName)
                        }
                    }
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateWorkoutDrawer(
    workout: Workout,
    onCancel: () -> Unit,
    onUpdate: (String) -> Unit
) {
    var workoutName by remember { mutableStateOf(workout.name) }
    var isNameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Update Workout",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = workoutName,
                onValueChange = {
                    workoutName = it
                    isNameError = it.isBlank()
                },
                label = { Text("Workout Name") },
                isError = isNameError,
                supportingText = {
                    if (isNameError) {
                        Text("Name cannot be empty")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (workoutName.isBlank()) {
                            isNameError = true
                        } else {
                            onUpdate(workoutName)
                        }
                    },
                    enabled = workoutName != workout.name && workoutName.isNotBlank()
                ) {
                    Text("Update")
                }
            }
        }
    }
}