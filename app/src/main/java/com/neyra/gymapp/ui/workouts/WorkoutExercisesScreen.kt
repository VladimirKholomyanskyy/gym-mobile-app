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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.openapi.models.Exercise
import com.neyra.gymapp.openapi.models.WorkoutExerciseResponse
import com.neyra.gymapp.openapi.models.WorkoutResponse
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.viewmodel.WorkoutExercisesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExercisesScreen(
    workout: WorkoutResponse,
    onWorkoutSelected: (workoutId: String) -> Unit,
    onStartWorkoutSession: (sessionId: String) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: WorkoutExercisesViewModel = hiltViewModel()
) {
    LaunchedEffect(workout) {
        viewModel.fetch(workout.id)
    }

    val workouts by viewModel.workoutExercises.collectAsState()
    val exercises by viewModel.exercisesMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedWorkoutExercise = viewModel.selectedWorkoutExercise.collectAsState()

    val isCreateWorkoutExerciseDrawerVisible by viewModel.isCreateWorkoutExerciseDrawerVisible.collectAsState()
    val isUpdateWorkoutExerciseDrawerVisible by viewModel.isUpdateWorkoutExerciseDrawerVisible.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = workout.name) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.startWorkoutSession(
                                workout.id,
                                onStartWorkoutSession
                            )
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Workout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateWorkoutExerciseDrawer() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!errorMessage.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage!!, color = Color.Red)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(workouts) { workout ->
                    exercises[workout.exerciseId]?.let { exercise ->
                        WorkoutExerciseCard(
                            workoutExerciseResponse = workout,
                            exercise = exercise,
                            onClick = { onWorkoutSelected(workout.id) },
                            onEdit = {
                                viewModel.setSelectedWorkoutExercise(workout)
                                viewModel.showUpdateWorkoutExerciseDrawer()
                            },
                            onDelete = { viewModel.removeExerciseFromWorkout(workout.id) }
                        )
                    }
                }
            }
        }

        if (isCreateWorkoutExerciseDrawerVisible) {
            CreateExerciseDrawer(
                viewModel = viewModel,
                onClose = { viewModel.hideCreateWorkoutExerciseDrawer() },
                onCreate = { exerciseId, sets, reps ->
                    viewModel.addExerciseToWorkout(
                        workout.id,
                        exerciseId,
                        sets,
                        reps
                    )
                }
            )
        }

        if (isUpdateWorkoutExerciseDrawerVisible) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideUpdateWorkoutExerciseDrawer() }
            ) {
                selectedWorkoutExercise.value?.let { workoutExercise ->
                    UpdateExerciseDrawer(
                        viewModel = viewModel,
                        selectedWorkoutExercise = workoutExercise,
                        onClose = { viewModel.hideUpdateWorkoutExerciseDrawer() },
                        onUpdate = { exerciseId, sets, reps ->
                            viewModel.updateExerciseInWorkout(
                                workout.id,
                                workoutExercise.id,
                                exerciseId,
                                sets,
                                reps
                            )
                        }
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExerciseCard(
    workoutExerciseResponse: WorkoutExerciseResponse,
    exercise: Exercise,
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${workoutExerciseResponse.sets}x${workoutExerciseResponse.reps}",
                    style = MaterialTheme.typography.titleLarge
                )
                // Optionally, list additional data like exercises if available.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDrawer(
    viewModel: WorkoutExercisesViewModel,
    onClose: () -> Unit,
    onCreate: (String, Int, Int) -> Unit
) {
    val exercises by viewModel.exercisesMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onClose
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Add Exercise", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (!errorMessage.isNullOrEmpty()) {
                Text(text = errorMessage!!, color = Color.Red)
            } else {
                // Exercise Selection Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedExercise?.name ?: "Select Exercise",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise") },
                        trailingIcon = {
                            IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                Icon(
                                    imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        exercises.values.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise.name) },
                                onClick = {
                                    selectedExercise = exercise
                                    isDropdownExpanded = false // Close dropdown after selection
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reps Input
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sets Input
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons: Cancel and Save
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onClose, // Close the bottom sheet
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val exerciseId = selectedExercise?.id
                            val repsInt = reps.toIntOrNull()
                            val setsInt = sets.toIntOrNull()

                            if (exerciseId != null && repsInt != null && setsInt != null) {
                                onCreate(exerciseId, repsInt, setsInt)
                                onClose() // Close the bottom sheet
                            } else {
                                println("Invalid input: Ensure all fields are filled correctly.")
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateExerciseDrawer(
    viewModel: WorkoutExercisesViewModel,
    selectedWorkoutExercise: WorkoutExerciseResponse,
    onClose: () -> Unit,
    onUpdate: (String, Int, Int) -> Unit
) {
    val exercises by viewModel.exercisesMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedExercise by remember { mutableStateOf(exercises[selectedWorkoutExercise.exerciseId]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf(selectedWorkoutExercise.reps.toString()) }
    var sets by remember { mutableStateOf(selectedWorkoutExercise.sets.toString()) }

    ModalBottomSheet(
        onDismissRequest = onClose
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Update Exercise", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else if (!errorMessage.isNullOrEmpty()) {
                Text(text = errorMessage!!, color = Color.Red)
            } else {
                // Exercise Selection Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedExercise?.name ?: "Select Exercise",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise") },
                        trailingIcon = {
                            IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                Icon(
                                    imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        exercises.values.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise.name) },
                                onClick = {
                                    selectedExercise = exercise
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reps Input
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sets Input
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons: Cancel and Save
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onClose, // Close the bottom sheet
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val exerciseId = selectedExercise?.id
                            val repsInt = reps.toIntOrNull()
                            val setsInt = sets.toIntOrNull()

                            if (exerciseId != null && repsInt != null && setsInt != null) {
                                onUpdate(exerciseId, repsInt, setsInt)
                                onClose() // Close the bottom sheet
                            } else {
                                println("Invalid input: Ensure all fields are filled correctly.")
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}




