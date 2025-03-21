package com.neyra.gymapp.ui.programs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.common.UiState
import com.neyra.gymapp.domain.model.TrainingProgram
import com.neyra.gymapp.ui.components.ConfirmationBottomDrawer
import com.neyra.gymapp.ui.theme.AccentMagenta
import com.neyra.gymapp.ui.theme.AccentNeonBlue
import com.neyra.gymapp.ui.theme.AccentNeonGreen
import com.neyra.gymapp.ui.theme.AccentPurple
import com.neyra.gymapp.ui.theme.BackgroundPrimary
import com.neyra.gymapp.ui.theme.BackgroundSecondary
import com.neyra.gymapp.ui.theme.CardBackground
import com.neyra.gymapp.ui.theme.StatusError
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
    val isDeleteConfirmationVisible by viewModel.isDeleteConfirmationVisible.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text("Training programs", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSecondary,
                    titleContentColor = AccentMagenta
                ),
                actions = {
                    // Calendar icon to navigate to calendar view
                    IconButton(onClick = onCalendarClicked) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Open Calendar",
                            tint = AccentNeonBlue
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateProgramDrawer() },
                containerColor = AccentMagenta,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.shadow(
                    elevation = 10.dp,
                    spotColor = AccentMagenta.copy(alpha = 0.8f),
                    ambientColor = AccentMagenta.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Training Program",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundPrimary)
        ) {
            when (val state = programsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AccentMagenta
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyStateContent()
                    } else {
                        TrainingProgramsList(
                            programs = state.data,
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

        // Delete Confirmation Dialog
        if (isDeleteConfirmationVisible) {
            selectedProgram?.let { program ->
                ConfirmationBottomDrawer(
                    message = "Are you sure you want to delete \"${program.name}\"?",
                    confirmButtonText = "Delete",
                    cancelButtonText = "Cancel",
                    onConfirm = {
                        program.id?.let { id ->
                            viewModel.deleteTrainingProgram(id)
                        }
                    },
                    onCancel = { viewModel.hideDeleteConfirmation() }
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EmptyStateContent(
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
                imageVector = Icons.Default.Add,
                contentDescription = "Add Training Program",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentMagenta.copy(alpha = 0.7f),
                                AccentMagenta.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No training programs",
                style = MaterialTheme.typography.titleLarge,
                color = AccentMagenta,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap the '+' button to create your first training program",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
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
                    .size(80.dp)
                    .padding(bottom = 16.dp),
                tint = StatusError
            )
            Text(
                text = "SYSTEM MALFUNCTION",
                style = MaterialTheme.typography.titleLarge,
                color = StatusError,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentNeonGreen,
                    contentColor = Color.Black
                ),
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    spotColor = AccentNeonGreen.copy(alpha = 0.8f),
                    ambientColor = AccentNeonGreen.copy(alpha = 0.4f)
                )
            ) {
                Text("REBOOT SYSTEM", fontWeight = FontWeight.Bold)
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
            .clickable(onClick = onClick)
            .shadow(
                elevation = 8.dp,
                spotColor = AccentMagenta.copy(alpha = 0.5f),
                ambientColor = AccentMagenta.copy(alpha = 0.2f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
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
                    color = AccentNeonBlue
                )
                program.description?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                // Display program complexity with cyberpunk styling
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val complexityColor = when (program.getProgramComplexity()) {
                        TrainingProgram.ProgramComplexity.BEGINNER -> AccentNeonGreen
                        TrainingProgram.ProgramComplexity.INTERMEDIATE -> AccentNeonBlue
                        TrainingProgram.ProgramComplexity.ADVANCED -> AccentMagenta
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(complexityColor, CircleShape)
                            .shadow(
                                elevation = 4.dp,
                                spotColor = complexityColor,
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = program.getProgramComplexity().name,
                        style = MaterialTheme.typography.bodySmall,
                        color = complexityColor
                    )
                }
            }

            // More options button
            IconButton(
                onClick = { isOptionsExpanded = true }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Program Options",
                    tint = AccentMagenta
                )
            }
        }
    }

    // Options Bottom Sheet
    if (isOptionsExpanded) {
        ModalBottomSheet(
            onDismissRequest = { isOptionsExpanded = false },
            containerColor = BackgroundSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentMagenta,
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider(color = AccentMagenta.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        onEdit()
                        isOptionsExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AccentNeonBlue
                    )
                ) {
                    Text(
                        "Edit program",
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        onDelete()
                        isOptionsExpanded = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = StatusError
                    )
                ) {
                    Text(
                        "Delete program",
                        fontWeight = FontWeight.Bold
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
        onDismissRequest = onCancel,
        containerColor = BackgroundSecondary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Create training program",
                style = MaterialTheme.typography.titleLarge,
                color = AccentMagenta,
                modifier = Modifier.padding(bottom = 24.dp),
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Program Name", color = AccentNeonBlue) },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(it, color = StatusError)
                    } ?: Text("${name.length}/${TrainingProgram.MAX_NAME_LENGTH}")
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundSecondary,
                    unfocusedContainerColor = BackgroundSecondary,
                    focusedIndicatorColor = AccentMagenta,
                    unfocusedIndicatorColor = AccentMagenta.copy(alpha = 0.5f),
                    cursorColor = AccentMagenta,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = AccentNeonBlue,
                    unfocusedLabelColor = AccentNeonBlue.copy(alpha = 0.7f),
                    errorCursorColor = StatusError,
                    errorIndicatorColor = StatusError
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)", color = AccentNeonBlue) },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError != null,
                supportingText = {
                    descriptionError?.let {
                        Text(it, color = StatusError)
                    } ?: Text("${description.length}/${TrainingProgram.MAX_DESCRIPTION_LENGTH}")
                },
                minLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundSecondary,
                    unfocusedContainerColor = BackgroundSecondary,
                    focusedIndicatorColor = AccentMagenta,
                    unfocusedIndicatorColor = AccentMagenta.copy(alpha = 0.5f),
                    cursorColor = AccentMagenta,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = AccentNeonBlue,
                    unfocusedLabelColor = AccentNeonBlue.copy(alpha = 0.7f),
                    errorCursorColor = StatusError,
                    errorIndicatorColor = StatusError
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentNeonBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.shadow(
                        elevation = 4.dp,
                        spotColor = AccentNeonBlue.copy(alpha = 0.8f),
                        ambientColor = AccentNeonBlue.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
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
        onDismissRequest = onCancel,
        containerColor = BackgroundSecondary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Update training program",
                style = MaterialTheme.typography.titleLarge,
                color = AccentPurple,
                modifier = Modifier.padding(bottom = 24.dp),
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Program Name", color = AccentNeonBlue) },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(it, color = StatusError)
                    } ?: Text("${name.length}/${TrainingProgram.MAX_NAME_LENGTH}")
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundSecondary,
                    unfocusedContainerColor = BackgroundSecondary,
                    focusedIndicatorColor = AccentPurple,
                    unfocusedIndicatorColor = AccentPurple.copy(alpha = 0.5f),
                    cursorColor = AccentPurple,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = AccentNeonBlue,
                    unfocusedLabelColor = AccentNeonBlue.copy(alpha = 0.7f),
                    errorCursorColor = StatusError,
                    errorIndicatorColor = StatusError
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)", color = AccentNeonBlue) },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError != null,
                supportingText = {
                    descriptionError?.let {
                        Text(it, color = StatusError)
                    } ?: Text("${description.length}/${TrainingProgram.MAX_DESCRIPTION_LENGTH}")
                },
                minLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundSecondary,
                    unfocusedContainerColor = BackgroundSecondary,
                    focusedIndicatorColor = AccentPurple,
                    unfocusedIndicatorColor = AccentPurple.copy(alpha = 0.5f),
                    cursorColor = AccentPurple,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = AccentNeonBlue,
                    unfocusedLabelColor = AccentNeonBlue.copy(alpha = 0.7f),
                    errorCursorColor = StatusError,
                    errorIndicatorColor = StatusError
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
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
                    enabled = hasChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple,
                        contentColor = Color.White,
                        disabledContainerColor = AccentPurple.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.shadow(
                        elevation = 4.dp,
                        spotColor = AccentPurple.copy(alpha = 0.8f),
                        ambientColor = AccentPurple.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}