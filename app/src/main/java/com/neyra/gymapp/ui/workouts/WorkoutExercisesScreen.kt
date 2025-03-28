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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.domain.model.WorkoutExercise
import com.neyra.gymapp.openapi.models.Exercise
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.ui.components.EmptyStateContent
import com.neyra.gymapp.ui.components.LoadingScreen
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
    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load workout and exercises when the screen appears
    LaunchedEffect(workoutId) {
        viewModel.fetch(workoutId)
    }

    // Show error message in snackbar if present
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(uiState.workout?.name ?: "Workout Exercises")
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
                uiState.isLoading && uiState.exercises.isEmpty() -> {
                    LoadingScreen()
                }

                uiState.exercises.isEmpty() -> {
                    EmptyStateContent(
                        title = "No exercises added yet",
                        message = "Add your first exercise to get started",
                        buttonText = "Add Exercise",
                        onButtonClick = { viewModel.showCreateWorkoutExerciseDrawer() }
                    )
                }

                else -> {
                    ExerciseList(
                        exercises = uiState.exercises,
                        onExerciseClick = { exercise ->
                            exercise.exercise.id?.let { onWorkoutSelected(it) }
                        },
                        onEditExercise = { exercise ->
                            viewModel.setSelectedWorkoutExercise(exercise)
                            viewModel.showUpdateWorkoutExerciseDrawer()
                        },
                        onDeleteExercise = { exercise ->
                            viewModel.setSelectedWorkoutExercise(exercise)
                            viewModel.showDeleteConfirmation()
                        }
                    )
                }
            }

            // Loading indicator overlay for operations
            if (uiState.isLoading && uiState.exercises.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingScreen()
                }
            }
        }

        // Create Exercise Drawer
        if (uiState.isCreateExerciseDrawerVisible) {
            CreateExerciseDrawer(
                availableExercises = uiState.availableExercises.values.toList(),
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
        if (uiState.isUpdateExerciseDrawerVisible && uiState.selectedExercise != null) {
            UpdateExerciseDrawer(
                selectedExercise = uiState.selectedExercise!!,
                availableExercises = uiState.availableExercises.values.toList(),
                onClose = { viewModel.hideUpdateWorkoutExerciseDrawer() },
                onUpdate = { exerciseId, sets, reps ->
                    uiState.selectedExercise?.id?.let { id ->
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

        // Delete Confirmation Dialog
        if (uiState.isDeleteConfirmationVisible && uiState.selectedExercise != null) {
            ConfirmationBottomDrawer(
                message = "Are you sure you want to remove this exercise?",
                onConfirm = {
                    uiState.selectedExercise?.id?.let { id ->
                        viewModel.removeExerciseFromWorkout(id)
                    }
                },
                onCancel = { viewModel.hideDeleteConfirmation() }
            )
        }
    }
}

@Composable
fun ExerciseList(
    exercises: List<WorkoutExercise>,
    onExerciseClick: (WorkoutExercise) -> Unit,
    onEditExercise: (WorkoutExercise) -> Unit,
    onDeleteExercise: (WorkoutExercise) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(
            items = exercises,
            key = { it.id ?: it.hashCode().toString() }
        ) { exercise ->
            WorkoutExerciseCard(
                workoutExercise = exercise,
                onClick = { onExerciseClick(exercise) },
                onEdit = { onEditExercise(exercise) },
                onDelete = { onDeleteExercise(exercise) }
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                    text = workoutExercise.exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${workoutExercise.sets} sets × ${workoutExercise.reps} reps",
                    style = MaterialTheme.typography.bodyMedium
                )
                workoutExercise.exercise.primaryMuscle.let {
                    if (it.isNotEmpty()) {
                        Text(
                            text = "Targets: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        onDelete()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDrawer(
    availableExercises: List<Exercise>,
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

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    availableExercises.forEach { exercise ->
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

            // Buttons: Cancel and Add
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
    selectedExercise: WorkoutExercise,
    availableExercises: List<Exercise>,
    onClose: () -> Unit,
    onUpdate: (String, Int, Int) -> Unit
) {
    // Pre-select values from the current exercise
    var currentExercise by remember {
        mutableStateOf(availableExercises.find { it.id.toString() == selectedExercise.exercise.id })
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf(selectedExercise.reps.toString()) }
    var sets by remember { mutableStateOf(selectedExercise.sets.toString()) }
    var isInputValid by remember { mutableStateOf(true) }  // Prefilled with valid data
    var hasChanges by remember { mutableStateOf(false) }

    // Validate input and check for changes whenever values change
    LaunchedEffect(currentExercise, sets, reps) {
        isInputValid = currentExercise != null &&
                sets.isNotBlank() && sets.toIntOrNull() != null && sets.toInt() > 0 &&
                reps.isNotBlank() && reps.toIntOrNull() != null && reps.toInt() > 0

        hasChanges = (currentExercise?.id.toString() != selectedExercise.exercise.id) ||
                (sets.toIntOrNull() != selectedExercise.sets) ||
                (reps.toIntOrNull() != selectedExercise.reps)
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

            // Exercise Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentExercise?.name ?: "Select Exercise",
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
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    availableExercises.forEach { exercise ->
                        DropdownMenuItem(
                            text = { Text(exercise.name) },
                            onClick = {
                                currentExercise = exercise
                                isDropdownExpanded = false
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
                            currentExercise?.id?.let { exerciseId ->
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