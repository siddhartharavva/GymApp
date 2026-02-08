package com.example.gymtrackerphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.TextButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymtrackerphone.data.model.WorkoutUi
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyWorkoutsScreen(
    viewModel: WorkoutViewModel,
    onWorkoutClick: (WorkoutUi) -> Unit,
) {
    val workouts by viewModel.workouts.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var editingWorkoutId by remember { mutableStateOf<Int?>(null) }
    var inputBarHeight by remember { mutableStateOf(0.dp) }
    var workoutToDelete by remember { mutableStateOf<WorkoutUi?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importTemplateCsv(context, uri)
        }
    }

    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    val navBarHeight = 80.dp
    val maxLiftPx = with(density) {
        (imeBottomPx.toDp() - navBarHeight).coerceAtLeast(0.dp).toPx()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box {
            // ---------- LIST ----------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = inputBarHeight + 8.dp
                    )
            ) {
                Spacer(Modifier.height(8.dp))

                LazyColumn {
                    items(workouts, key = { it.id }) { workout ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.sendWorkoutToWatch(
                                        context = context,
                                        workoutId = workout.id
                                    )
                                    false // snap back, don't dismiss visually
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        "Send to Watch",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { onWorkoutClick(workout) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = workout.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = androidx.compose.ui.graphics.Color.White
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                textInput = workout.name
                                                editingWorkoutId = workout.id
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Edit, "Edit")
                                        }

                                        IconButton(
                                            onClick = { workoutToDelete = workout }
                                        ) {
                                            Icon(Icons.Outlined.Delete, "Delete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------- INPUT BAR ----------
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged {
                        with(density) {
                            inputBarHeight = it.height.toDp()
                        }
                    }
                    .graphicsLayer {
                        translationY =
                            if (imeBottomPx > 0) -maxLiftPx else 0f
                    },
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { importLauncher.launch("text/*") }
                        ) {
                            Text("Import CSV", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Workout name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )

                    Spacer(Modifier.height(4.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (textInput.isBlank()) return@Button

                            if (editingWorkoutId == null) {
                                viewModel.addWorkout(textInput)
                            } else {
                                viewModel.updateWorkout(editingWorkoutId!!, textInput)
                                editingWorkoutId = null
                            }

                            textInput = ""
                        }
                    ) {
                        Text(
                            if (editingWorkoutId == null)
                                "Add Workout"
                            else
                                "Update Workout"
                        )
                    }
                }
            }

            if (workoutToDelete != null) {
                AlertDialog(
                    onDismissRequest = { workoutToDelete = null },
                    title = { Text("Delete workout?") },
                    text = {
                        Text(
                            "This will remove the workout and its exercises/sets. " +
                                "Past history for this workout will no longer match."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val id = workoutToDelete?.id
                                if (id != null) {
                                    viewModel.deleteWorkout(id)
                                }
                                workoutToDelete = null
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { workoutToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
