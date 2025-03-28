package com.neyra.gymapp.ui.programs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.ui.components.EmptyStateContent
import com.neyra.gymapp.ui.components.LoadingScreen
import com.neyra.gymapp.viewmodel.TrainingProgramsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingProgramsScreen(
    onProgramSelected: (programId: String) -> Unit,
    onCalendarClicked: () -> Unit,
    viewModel: TrainingProgramsViewModel = hiltViewModel()
) {
    // Collect UI state
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
            TopAppBar(
                title = { Text("Training Programs") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // Calendar icon to navigate to calendar view
                    IconButton(onClick = onCalendarClicked) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Open Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateProgramDrawer() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Training Program",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.programs.isEmpty() -> {
                    LoadingScreen()
                }

                uiState.programs.isEmpty() -> {
                    EmptyStateContent(
                        title = "No training programs",
                        message = "Tap the '+' button to create your first training program",
                        buttonText = "Add Training Program",
                        onButtonClick = { viewModel.showCreateProgramDrawer() }
                    )
                }

                else -> {
                    TrainingProgramList(
                        programs = uiState.programs,
                        onProgramSelected = onProgramSelected,
                        onEditProgram = { program ->
                            viewModel.setSelectedProgram(program)
                            viewModel.showUpdateProgramDrawer()
                        },
                        onDeleteProgram = { program ->
                            viewModel.setSelectedProgram(program)
                            viewModel.showDeleteConfirmation()
                        }
                    )
                }
            }

            // Loading indicator overlay for operations
            if (uiState.isLoading && uiState.programs.isNotEmpty()) {
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

        // Create Program Drawer
        if (uiState.isCreateProgramDrawerVisible) {
            CreateTrainingProgramDrawer(
                onCancel = { viewModel.hideCreateProgramDrawer() },
                onCreate = { name, description ->
                    viewModel.createTrainingProgram(name, description)
                }
            )
        }

        // Update Program Drawer
        if (uiState.isUpdateProgramDrawerVisible && uiState.selectedProgram != null) {
            UpdateTrainingProgramDrawer(
                program = uiState.selectedProgram!!,
                onCancel = { viewModel.hideUpdateProgramDrawer() },
                onUpdate = { name, description ->
                    uiState.selectedProgram?.id?.let { id ->
                        viewModel.updateTrainingProgram(id, name, description)
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (uiState.isDeleteConfirmationVisible && uiState.selectedProgram != null) {
            ConfirmationBottomDrawer(
                message = "Are you sure you want to delete \"${uiState.selectedProgram?.name}\"?",
                confirmButtonText = "Delete",
                cancelButtonText = "Cancel",
                onConfirm = {
                    uiState.selectedProgram?.id?.let { id ->
                        viewModel.deleteTrainingProgram(id)
                    }
                },
                onCancel = { viewModel.hideDeleteConfirmation() }
            )
        }
    }
}

@Composable
fun TrainingProgramList(
    programs: List<TrainingProgram>,
    onProgramSelected: (String) -> Unit,
    onEditProgram: (TrainingProgram) -> Unit,
    onDeleteProgram: (TrainingProgram) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(
            items = programs,
            key = { it.id ?: it.hashCode().toString() }
        ) { program ->
            TrainingProgramCard(
                program = program,
                onClick = { program.id?.let { onProgramSelected(it) } },
                onEdit = { onEditProgram(program) },
                onDelete = { onDeleteProgram(program) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingProgramCard(
    program: TrainingProgram,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isOptionsExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                program.description?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Display program complexity
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = program.getProgramComplexity().name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // More options button
            IconButton(onClick = { isOptionsExpanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Program Options",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Options Bottom Sheet
    if (isOptionsExpanded) {
        ModalBottomSheet(
            onDismissRequest = { isOptionsExpanded = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider()

                TextButton(
                    onClick = {
                        isOptionsExpanded = false
                        onEdit()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Edit program",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                TextButton(
                    onClick = {
                        isOptionsExpanded = false
                        onDelete()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Delete program",
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
fun CreateTrainingProgramDrawer(
    onCancel: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        if (name.isBlank()) {
            nameError = "Program name cannot be empty"
            isValid = false
        } else if (name.length > TrainingProgram.MAX_NAME_LENGTH) {
            nameError = "Name must be ${TrainingProgram.MAX_NAME_LENGTH} characters or less"
            isValid = false
        } else {
            nameError = null
        }

        // Validate description (optional field)
        if (description.length > TrainingProgram.MAX_DESCRIPTION_LENGTH) {
            descriptionError =
                "Description must be ${TrainingProgram.MAX_DESCRIPTION_LENGTH} characters or less"
            isValid = false
        } else {
            descriptionError = null
        }

        return isValid
    }

    ModalBottomSheet(
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Create training program",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Program Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("${name.length}/${TrainingProgram.MAX_NAME_LENGTH}")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError != null,
                supportingText = {
                    descriptionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("${description.length}/${TrainingProgram.MAX_DESCRIPTION_LENGTH}")
                },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (validateInputs()) {
                            onCreate(
                                name,
                                description.takeIf { it.isNotBlank() }
                            )
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
fun UpdateTrainingProgramDrawer(
    program: TrainingProgram,
    onCancel: () -> Unit,
    onUpdate: (String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(program.name) }
    var description by remember { mutableStateOf(program.description ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(name, description) {
        hasChanges = name != program.name || description != (program.description ?: "")
    }

    fun validateInputs(): Boolean {
        var isValid = true

        // Validate name
        if (name.isBlank()) {
            nameError = "Program name cannot be empty"
            isValid = false
        } else if (name.length > TrainingProgram.MAX_NAME_LENGTH) {
            nameError = "Name must be ${TrainingProgram.MAX_NAME_LENGTH} characters or less"
            isValid = false
        } else {
            nameError = null
        }

        // Validate description (optional field)
        if (description.length > TrainingProgram.MAX_DESCRIPTION_LENGTH) {
            descriptionError =
                "Description must be ${TrainingProgram.MAX_DESCRIPTION_LENGTH} characters or less"
            isValid = false
        } else {
            descriptionError = null
        }

        return isValid
    }

    ModalBottomSheet(
        onDismissRequest = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Update training program",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Program Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("${name.length}/${TrainingProgram.MAX_NAME_LENGTH}")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError != null,
                supportingText = {
                    descriptionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    } ?: Text("${description.length}/${TrainingProgram.MAX_DESCRIPTION_LENGTH}")
                },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        if (validateInputs()) {
                            val updatedName = if (name != program.name) name else null
                            val updatedDescription = if (description != (program.description ?: ""))
                                description.takeIf { it.isNotBlank() } else null

                            onUpdate(updatedName, updatedDescription)
                        }
                    },
                    enabled = hasChanges
                ) {
                    Text("Update")
                }
            }
        }
    }
}