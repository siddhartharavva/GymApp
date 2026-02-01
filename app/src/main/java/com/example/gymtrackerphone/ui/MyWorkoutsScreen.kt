package com.example.gymtrackerphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                Text(
                    text = "My Workouts",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn {
                    items(workouts) { workout ->
                        val context = LocalContext.current

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
                                    Text(workout.name)

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
                                            onClick = {
                                                viewModel.deleteWorkout(workout.id)
                                            }
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
                tonalElevation = 3.dp // optional but looks clean
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {

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
            }
        }
}
