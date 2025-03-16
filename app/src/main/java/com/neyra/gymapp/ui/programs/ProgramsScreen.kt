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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.openapi.models.TrainingProgram
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.viewmodel.TrainingProgramsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onTrainingSelected: (program: TrainingProgram) -> Unit,
    onCalendarClicked: () -> Unit,
    viewModel: TrainingProgramsViewModel = hiltViewModel()
) {
    val programsState = viewModel.programs.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()
    val isCreateProgramDrawerVisible by viewModel.isCreateProgramDrawerVisible.collectAsState()
    val isUpdateProgramDrawerVisible by viewModel.isUpdateProgramDrawerVisible.collectAsState()
    val selectedProgram = viewModel.selectedProgram.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Programs") },
                actions = {
                    IconButton(onClick = onCalendarClicked) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Open Calendar")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(programsState.value) { program ->
                        TrainingProgramCard(
                            program = program,
                            onClick = { onTrainingSelected(program) },
                            onEdit = {
                                viewModel.setSelectedProgram(program)
                                viewModel.showUpdateProgramDrawer()
                            },
                            onDelete = { viewModel.deleteTrainingProgram(program.id) }
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { viewModel.showCreateProgramDrawer() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Training Program")
                }
            }

            // Create Drawer
            if (isCreateProgramDrawerVisible) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.hideCreateProgramDrawer() }
                ) {
                    CreateTrainingProgramDrawer(
                        onCancel = { viewModel.hideCreateProgramDrawer() },
                        onCreate = { name, description ->
                            viewModel.createTrainingProgram(name, description)
                        }
                    )
                }
            }

            // Update Drawer
            if (isUpdateProgramDrawerVisible) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.hideUpdateProgramDrawer() }
                ) {
                    selectedProgram.value?.let { program ->
                        UpdateTrainingProgramDrawer(
                            program = program,
                            onCancel = { viewModel.hideUpdateProgramDrawer() },
                            onUpdate = { updatedName, updatedDescription ->
                                viewModel.updateTrainingProgram(
                                    program.id,
                                    updatedName ?: program.name,
                                    updatedDescription ?: program.description
                                )
                            }
                        )
                    }
                }
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
                    text = program.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 20.sp
                )
                program.description?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
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

    // Bottom Drawer for Edit/Delete Options
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
            message = "Are you sure you want to delete this training program?",
            onConfirm = {
                onDelete()
                isConfirmationVisible = false
            },
            onCancel = { isConfirmationVisible = false }
        )
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
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
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
                        onCreate(name, description.ifBlank { null })
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
    var newName by remember { mutableStateOf(program.name) }
    var newDescription by remember { mutableStateOf(program.description ?: "") }

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
            value = newName,
            onValueChange = { newName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = newDescription,
            onValueChange = { newDescription = it },
            label = { Text("Description (Optional)") },
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
                val name = if (newName != program.name) newName else null
                val description =
                    if (newDescription != program.description) newDescription else null
                onUpdate(name, description)
            }) {
                Text("Update")
            }
        }
    }
}