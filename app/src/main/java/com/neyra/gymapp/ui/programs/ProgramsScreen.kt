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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.viewmodel.TrainingProgramsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingProgramsScreen(
    onProgramSelected: (programId: String) -> Unit,
    onCalendarClicked: () -> Unit,
    viewModel: TrainingProgramsViewModel = hiltViewModel()
) {
    val programsState by viewModel.programs.collectAsState()
    val isCreateProgramDrawerVisible by viewModel.isCreateProgramDrawerVisible.collectAsState()
    val isUpdateProgramDrawerVisible by viewModel.isUpdateProgramDrawerVisible.collectAsState()
    val selectedProgram by viewModel.selectedProgram.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Training Programs") },
                actions = {
                    // Calendar icon to navigate to calendar view
                    androidx.compose.material3.IconButton(onClick = onCalendarClicked) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Open Calendar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateProgramDrawer() }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Training Program"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = programsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyStateContent(
                            onCreateProgram = { viewModel.showCreateProgramDrawer() }
                        )
                    } else {
                        TrainingProgramsList(
                            programs = state.data,
                            onProgramSelected = onProgramSelected,
                            onEditProgram = { program ->
                                viewModel.setSelectedProgram(program)
                                viewModel.showUpdateProgramDrawer()
                            },
                            onDeleteProgram = { program ->
                                viewModel.deleteTrainingProgram(
                                    program.id ?: return@TrainingProgramsList

                                )
                            }
                        )
                    }
                }

                is UiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.fetchTrainingPrograms() }
                    )
                }
            }
        }

        // Create Program Drawer
        if (isCreateProgramDrawerVisible) {
            CreateTrainingProgramDrawer(
                onCancel = { viewModel.hideCreateProgramDrawer() },
                onCreate = { name, description ->
                    viewModel.createTrainingProgram(name, description)
                }
            )
        }

        // Update Program Drawer
        if (isUpdateProgramDrawerVisible) {
            selectedProgram?.let { program ->
                UpdateTrainingProgramDrawer(
                    program = program,
                    onCancel = { viewModel.hideUpdateProgramDrawer() },
                    onUpdate = { name, description ->
                        viewModel.updateTrainingProgram(
                            program.id ?: return@UpdateTrainingProgramDrawer,
                            name,
                            description
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun TrainingProgramsList(
    programs: List<TrainingProgram>,
    onProgramSelected: (programId: String) -> Unit,
    onEditProgram: (TrainingProgram) -> Unit,
    onDeleteProgram: (TrainingProgram) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(programs) { program ->
            TrainingProgramCard(
                program = program,
                onClick = { onProgramSelected(program.id ?: "") },
                onEdit = { onEditProgram(program) },
                onDelete = { onDeleteProgram(program) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EmptyStateContent(
    onCreateProgram: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No Training Programs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Create your first training program to get started",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onCreateProgram) {
                Text("Create Program")
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
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
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium
                )
                program.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Display program complexity
                Text(
                    text = "Complexity: ${program.getProgramComplexity().name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // More options button
            androidx.compose.material3.IconButton(
                onClick = { isOptionsExpanded = true }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Program Options"
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
                TextButton(
                    onClick = {
                        onEdit()
                        isOptionsExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Program")
                }
                TextButton(
                    onClick = {
                        onDelete()
                        isOptionsExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Program")
                }
            }
        }
    }
}

@Composable
fun CreateTrainingProgramDrawer(
    onCancel: () -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
            label = { Text("Program Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description.takeIf { it.isNotBlank() })
                    }
                }
            ) {
                Text("Create")
            }
        }
    }
}

@Composable
fun UpdateTrainingProgramDrawer(
    program: TrainingProgram,
    onCancel: () -> Unit,
    onUpdate: (String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(program.name) }
    var description by remember { mutableStateOf(program.description ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Update Training Program",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Program Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val updatedName = name.takeIf { it != program.name }
                    val updatedDescription = description.takeIf { it != program.description }

                    // Only call update if something has changed
                    if (updatedName != null || updatedDescription != null) {
                        onUpdate(updatedName, updatedDescription)
                    }
                }
            ) {
                Text("Update")
            }
        }
    }
}