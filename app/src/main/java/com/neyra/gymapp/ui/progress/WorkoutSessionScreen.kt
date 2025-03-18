package com.neyra.gymapp.ui.progress

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.neyra.gymapp.viewmodel.WorkoutSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    sessionId: String,
    onBackPressed: () -> Unit,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
//    LaunchedEffect(sessionId) {
//        viewModel.fetchSession(sessionId)
//    }
//    var isSessionCompleted by remember { mutableStateOf(false) }
//    var showDialog by remember { mutableStateOf(false) }
//    val workoutSession by viewModel.workoutSession.collectAsState()
//    val exerciseInputs = remember(workoutSession) {
//        workoutSession?.workoutSnapshot?.workoutExercises?.associate { workoutExercise ->
//            val exerciseId = workoutExercise.exercise?.id.orEmpty()
//            exerciseId to mutableStateListOf<SetInput>().apply {
//                repeat(workoutExercise.sets) { add(SetInput()) }
//            }
//        } ?: emptyMap()
//    }
//    val pagerState = rememberPagerState(pageCount = {
//        workoutSession?.workoutSnapshot?.workoutExercises?.size ?: 0
//    })
//
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text("Workout Session") },
//                actions = {
//                    Button(
//                        onClick = { showDialog = true },
//                    ) {
//                        Text("Complete Session")
//                    }
//                }
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            workoutSession?.let { session ->
//                WorkoutTimer(
//                    startedAt = session.startedAt.toInstant().toEpochMilli(),
//                    isSessionCompleted = isSessionCompleted
//                )
//            }
//            HorizontalPager(
//                state = pagerState,
//                modifier = Modifier.weight(1f)
//            ) { page ->
//                workoutSession?.workoutSnapshot?.workoutExercises?.let { exercises ->
//                    val workoutExercise = exercises[page]
//                    val exerciseId = workoutExercise.exercise?.id.orEmpty()
//                    val setInputs = exerciseInputs[exerciseId]
//                        ?: mutableStateListOf<SetInput>().apply {
//                            repeat(workoutExercise.sets) { add(SetInput()) }
//                        }
//                    Column(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.Top
//                    ) {
//                        WorkoutSessionExerciseCard(
//                            workoutExercise = workoutExercise,
//                            setInputs = setInputs
//                        )
//                    }
//                }
//            }
//            PagerIndicator(
//                pagerState = pagerState,
//                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
//                    .padding(16.dp)
//            )
//        }
//    }
//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        isSessionCompleted = true  // Stop timer
//                        viewModel.completeSession(sessionId, exerciseInputs) // Complete session
//                        showDialog = false
//                    }
//                ) {
//                    Text("Confirm")
//                }
//            },
//            dismissButton = {
//                Button(onClick = { showDialog = false }) {
//                    Text("Cancel")
//                }
//            },
//            title = { Text("Complete Session") },
//            text = { Text("Are you sure you want to complete this session?") }
//        )
//    }
}
//
//@Composable
//fun WorkoutTimer(startedAt: Long, isSessionCompleted: Boolean) {
//    var elapsedTime by remember { mutableStateOf(0L) }
//
//    LaunchedEffect(startedAt, isSessionCompleted) {
//        while (!isSessionCompleted) {
//            val currentTime = System.currentTimeMillis()
//            elapsedTime = currentTime - startedAt
//            delay(1000L)
//        }
//    }
//
//    val seconds = (elapsedTime / 1000) % 60
//    val minutes = (elapsedTime / (1000 * 60)) % 60
//    val hours = (elapsedTime / (1000 * 60 * 60))
//    val timeFormatted = if (hours > 0)
//        String.format("%02d:%02d:%02d", hours, minutes, seconds)
//    else
//        String.format("%02d:%02d", minutes, seconds)
//
//    Text(
//        text = "Workout Time: $timeFormatted",
//        style = MaterialTheme.typography.bodyLarge,
//        modifier = Modifier.padding(16.dp)
//    )
//}
//
//
//@Composable
//fun PagerIndicator(
//    pagerState: PagerState,
//    modifier: Modifier = Modifier,
//    activeColor: Color = MaterialTheme.colorScheme.primary,
//    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
//    indicatorSize: Dp = 8.dp,
//    spacing: Dp = 4.dp
//) {
//    Row(
//        modifier = modifier,
//        horizontalArrangement = Arrangement.Center,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        repeat(pagerState.pageCount) { index ->
//            Box(
//                modifier = Modifier
//                    .size(indicatorSize)
//                    .background(
//                        color = if (index == pagerState.currentPage) activeColor else inactiveColor,
//                        shape = CircleShape
//                    )
//            )
//            if (index < pagerState.pageCount - 1) {
//                Spacer(modifier = Modifier.width(spacing))
//            }
//        }
//    }
//}
//
//@Composable
//fun WorkoutSessionExerciseCard(
//    workoutExercise: WorkoutExercise,
//    setInputs: SnapshotStateList<SetInput>
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(16.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            workoutExercise.exercise?.let {
//                Text(
//                    text = it.name,
//                    style = MaterialTheme.typography.titleLarge
//                )
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            setInputs.forEachIndexed { index, setInput ->
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Text(
//                        text = "${index + 1}",
//                        modifier = Modifier.width(30.dp)
//                    )
//                    OutlinedTextField(
//                        value = setInput.reps.value,
//                        onValueChange = { newValue ->
//                            setInput.reps.value = newValue
//                        },
//                        trailingIcon = { Text("reps") },
//                        modifier = Modifier.weight(1f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                    )
//                    OutlinedTextField(
//                        value = setInput.weight.value,
//                        onValueChange = { newValue ->
//                            setInput.weight.value = newValue
//                        },
//                        trailingIcon = { Text("kg") },
//                        singleLine = true,
//                        modifier = Modifier.weight(1f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
//                    )
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//            }
//            Button(
//                onClick = { setInputs.add(SetInput()) },
//                modifier = Modifier
//                    .align(Alignment.End)
//                    .padding(bottom = 8.dp)
//            ) {
//                Text("Add Set")
//            }
//        }
//    }
//}

