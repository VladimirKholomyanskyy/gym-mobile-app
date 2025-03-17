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
import com.neyra.gymapp.openapi.models.WorkoutResponse
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.viewmodel.WorkoutsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    trainingProgramId: String,
    onWorkoutSelected: (workout: WorkoutResponse) -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel()
) {
    // Trigger fetching of workouts when the screen appears or programId changes.
    LaunchedEffect(program) {
        viewModel.fetchWorkouts(program.id)
    }

    val workouts by viewModel.workouts.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val isCreateWorkoutDrawerVisible by viewModel.isCreateWorkoutDrawerVisible.collectAsState()
    val isUpdateWorkoutDrawerVisible by viewModel.isUpdateWorkoutDrawerVisible.collectAsState()
    val selectedWorkout = viewModel.selectedWorkout.collectAsState()
    if (isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = program.name) },
                    navigationIcon = {
                        IconButton(onClick = { /* do something */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Display list of workouts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    items(workouts) { workout ->
                        WorkoutCard(workout = workout, onClick = { onWorkoutSelected(workout) },
                            onEdit = {
                                viewModel.setSelectedWorkout(workout)
                                viewModel.showUpdateWorkoutDrawer()
                            },
                            onDelete = { viewModel.deleteWorkout(program.id, workout.id) })
                    }
                }
                FloatingActionButton(
                    onClick = { viewModel.showCreateWorkoutDrawer() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Training Program")
                }
            }

        }
        // Create Drawer
        if (isCreateWorkoutDrawerVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideCreateWorkoutDrawer() }
            ) {
                CreateWorkoutDrawer(
                    onCancel = { viewModel.hideCreateWorkoutDrawer() },
                    onCreate = { name ->
                        viewModel.createWorkout(program.id, name)
                    }
                )
            }
        }

        // Update Drawer
        if (isUpdateWorkoutDrawerVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideUpdateWorkoutDrawer() }
            ) {
                selectedWorkout.value?.let { workout ->
                    UpdateWorkoutDrawer(
                        onCancel = { viewModel.hideUpdateWorkoutDrawer() },
                        onUpdate = { updatedName ->
                            updatedName?.let {
                                viewModel.updateWorkout(
                                    program.id,
                                    workout.id,
                                    updatedName
                                )
                            }

                        },
                        workout = workout
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCard(
    workout: WorkoutResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isDrawerVisible by remember { mutableStateOf(false) }
    var isConfirmationVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
            // Program Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleLarge
                )
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
                    Text("Delete", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
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

@Composable
fun CreateWorkoutDrawer(
    onCancel: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Create Training Program",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
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
                    if (name.isNotBlank()) {
                        onCreate(name)
                    }
                }
            ) {
                Text("Create")
            }
        }
    }
}


@Composable
fun UpdateWorkoutDrawer(
    workout: WorkoutResponse,
    onCancel: () -> Unit,
    onUpdate: (String?) -> Unit
) {
    var newName by remember { mutableStateOf(workout.name) }

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
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Name") },
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
            Button(onClick = {
                val name = if (newName != workout.name) newName else null
                onUpdate(name)
            }) {
                Text("Update")
            }
        }
    }
}