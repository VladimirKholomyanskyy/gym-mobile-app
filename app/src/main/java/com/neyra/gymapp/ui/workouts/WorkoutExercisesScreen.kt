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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.domain.model.Workout
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.openapi.models.Exercise
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.viewmodel.WorkoutExercisesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExercisesScreen(
    workoutId: String,
    onWorkoutSelected: (exerciseId: String) -> Unit,
    onBackPressed: () -> Unit,
    onStartWorkoutSession: (sessionId: String) -> Unit,
    viewModel: WorkoutExercisesViewModel = hiltViewModel()
) {
    // Load workout and exercises when the screen appears
    LaunchedEffect(workoutId) {
        viewModel.fetch(workoutId)
    }

    val workout by viewModel.workout.collectAsState()
    val workoutExercises by viewModel.workoutExercises.collectAsState()
    val exercisesMap by viewModel.exercisesMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isCreateWorkoutExerciseDrawerVisible by viewModel.isCreateWorkoutExerciseDrawerVisible.collectAsState()
    val isUpdateWorkoutExerciseDrawerVisible by viewModel.isUpdateWorkoutExerciseDrawerVisible.collectAsState()
    val selectedWorkoutExercise by viewModel.selectedWorkoutExercise.collectAsState()

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
                title = {
                    when (workout) {
                        is UiState.Success -> Text((workout as UiState.Success<Workout>).data.name)
                        else -> Text("Workout Exercises")
                    }
                },
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
                            viewModel.startWorkoutSession(workoutId, onStartWorkoutSession)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                workoutExercises is UiState.Error -> {
                    Text(
                        text = (workoutExercises as UiState.Error).message,
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                workoutExercises is UiState.Success -> {
                    val exercises =
                        (workoutExercises as UiState.Success<List<WorkoutExercise>>).data
                    if (exercises.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No exercises added yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add your first exercise to get started",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.showCreateWorkoutExerciseDrawer() }) {
                                Text("Add Exercise")
                            }
                        }
                    } else {
                        // List of exercises
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            items(exercises) { exercise ->
                                WorkoutExerciseCard(
                                    workoutExercise = exercise,
                                    onClick = { onWorkoutSelected(exercise.exerciseId) },
                                    onEdit = {
                                        viewModel.setSelectedWorkoutExercise(exercise)
                                        viewModel.showUpdateWorkoutExerciseDrawer()
                                    },
                                    onDelete = {
                                        exercise.id?.let { id ->
                                            viewModel.removeExerciseFromWorkout(id)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Create Exercise Drawer
        if (isCreateWorkoutExerciseDrawerVisible) {
            CreateExerciseDrawer(
                viewModel = viewModel,
                exercisesMap = exercisesMap,
                onClose = { viewModel.hideCreateWorkoutExerciseDrawerVisible() },
                onCreate = { exerciseId, sets, reps ->
                    viewModel.addExerciseToWorkout(
                        workoutId,
                        exerciseId,
                        sets,
                        reps
                    )
                }
            )
        }

        // Update Exercise Drawer
        if (isUpdateWorkoutExerciseDrawerVisible && selectedWorkoutExercise != null) {
            UpdateExerciseDrawer(
                viewModel = viewModel,
                selectedWorkoutExercise = selectedWorkoutExercise!!,
                exercisesMap = exercisesMap,
                onClose = { viewModel.hideUpdateWorkoutExerciseDrawer() },
                onUpdate = { exerciseId, sets, reps ->
                    selectedWorkoutExercise?.id?.let { id ->
                        viewModel.updateExerciseInWorkout(
                            workoutId,
                            id,
                            exerciseId,
                            sets,
                            reps
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExerciseCard(
    workoutExercise: WorkoutExercise,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workoutExercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${workoutExercise.sets} sets × ${workoutExercise.reps} reps",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (workoutExercise.primaryMuscle.isNotEmpty()) {
                    Text(
                        text = "Targets: ${workoutExercise.primaryMuscle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu Button
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
            message = "Are you sure you want to remove this exercise?",
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
    exercisesMap: Map<String, Exercise>,
    onClose: () -> Unit,
    onCreate: (String, Int, Int) -> Unit
) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var isInputValid by remember { mutableStateOf(false) }

    // Validate input whenever it changes
    LaunchedEffect(selectedExercise, sets, reps) {
        isInputValid = selectedExercise != null &&
                sets.isNotBlank() && sets.toIntOrNull() != null && sets.toInt() > 0 &&
                reps.isNotBlank() && reps.toIntOrNull() != null && reps.toInt() > 0
    }

    ModalBottomSheet(
        onDismissRequest = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add Exercise",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Exercise Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
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

                androidx.compose.material3.DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    exercisesMap.values.forEach { exercise ->
                        androidx.compose.material3.DropdownMenuItem(
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

            // Sets Input
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("Sets") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = sets.isNotBlank() && (sets.toIntOrNull() == null || sets.toInt() <= 0),
                supportingText = {
                    if (sets.isNotBlank() && (sets.toIntOrNull() == null || sets.toInt() <= 0)) {
                        Text("Enter a positive number")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reps Input
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = reps.isNotBlank() && (reps.toIntOrNull() == null || reps.toInt() <= 0),
                supportingText = {
                    if (reps.isNotBlank() && (reps.toIntOrNull() == null || reps.toInt() <= 0)) {
                        Text("Enter a positive number")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons: Cancel and Save
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isInputValid) {
                            selectedExercise?.id?.let { exerciseId ->
                                onCreate(
                                    exerciseId.toString(),
                                    sets.toInt(),
                                    reps.toInt()
                                )
                            }
                        }
                    },
                    enabled = isInputValid
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateExerciseDrawer(
    viewModel: WorkoutExercisesViewModel,
    selectedWorkoutExercise: WorkoutExercise,
    exercisesMap: Map<String, Exercise>,
    onClose: () -> Unit,
    onUpdate: (String, Int, Int) -> Unit
) {
    // Pre-select the current exercise
    var selectedExercise by remember {
        mutableStateOf(exercisesMap[selectedWorkoutExercise.exerciseId])
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf(selectedWorkoutExercise.reps.toString()) }
    var sets by remember { mutableStateOf(selectedWorkoutExercise.sets.toString()) }
    var isInputValid by remember { mutableStateOf(true) }  // Prefilled with valid data
    var hasChanges by remember { mutableStateOf(false) }

    // Validate input and check for changes whenever values change
    LaunchedEffect(selectedExercise, sets, reps) {
        isInputValid = selectedExercise != null &&
                sets.isNotBlank() && sets.toIntOrNull() != null && sets.toInt() > 0 &&
                reps.isNotBlank() && reps.toIntOrNull() != null && reps.toInt() > 0

        hasChanges = (selectedExercise?.id.toString() != selectedWorkoutExercise.exerciseId) ||
                (sets.toIntOrNull() != selectedWorkoutExercise.sets) ||
                (reps.toIntOrNull() != selectedWorkoutExercise.reps)
    }

    ModalBottomSheet(
        onDismissRequest = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Update Exercise",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Exercise Selection Dropdown - Pre-selected with current exercise
            Box(modifier = Modifier.fillMaxWidth()) {
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

                androidx.compose.material3.DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    exercisesMap.values.forEach { exercise ->
                        androidx.compose.material3.DropdownMenuItem(
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

            // Sets Input - Prefilled with current value
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it },
                label = { Text("Sets") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = sets.isNotBlank() && (sets.toIntOrNull() == null || sets.toInt() <= 0),
                supportingText = {
                    if (sets.isNotBlank() && (sets.toIntOrNull() == null || sets.toInt() <= 0)) {
                        Text("Enter a positive number")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reps Input - Prefilled with current value
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = reps.isNotBlank() && (reps.toIntOrNull() == null || reps.toInt() <= 0),
                supportingText = {
                    if (reps.isNotBlank() && (reps.toIntOrNull() == null || reps.toInt() <= 0)) {
                        Text("Enter a positive number")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons: Cancel and Update
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isInputValid && hasChanges) {
                            selectedExercise?.id?.let { exerciseId ->
                                onUpdate(
                                    exerciseId.toString(),
                                    sets.toInt(),
                                    reps.toInt()
                                )
                            }
                        }
                    },
                    enabled = isInputValid && hasChanges
                ) {
                    Text("Update")
                }
            }
        }
    }
}