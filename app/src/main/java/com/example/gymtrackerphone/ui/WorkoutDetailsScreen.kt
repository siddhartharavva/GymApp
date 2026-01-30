package com.example.gymtrackerphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel

@Composable
fun PlusMinusButton(
    symbol: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

fun formatRest(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    workoutId: Int,
    viewModel: WorkoutViewModel
) {
    val workout = viewModel.getWorkoutById(workoutId)

    Scaffold(
        modifier = Modifier.imePadding(),

        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout") }
            )
        },
        bottomBar = {
            var newExerciseName by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                OutlinedTextField(
                    value = newExerciseName,
                    onValueChange = { newExerciseName = it },
                    label = { Text("New Exercise") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (newExerciseName.isNotBlank() && workout != null) {
                            viewModel.addExercise(workout.id, newExerciseName)
                            newExerciseName = ""
                        }
                    }
                ) {
                    Text("Add Exercise")
                }
            }
        }
    ) { paddingValues ->

        if (workout == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Workout not found")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(workout.exercises) { exercise ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // ---- EXERCISE HEADER ----
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(
                                onClick = {
                                    viewModel.deleteExercise(workout.id, exercise.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete exercise"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ---- SETS ----
                        exercise.sets.forEachIndexed { index, set ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // Set label
                                Text(
                                    text = "Set ${index + 1}",
                                    modifier = Modifier.width(56.dp)
                                )

                                // Reps -
                                PlusMinusButton(
                                    symbol = "-",
                                    onClick = {
                                        viewModel.updateSet(
                                            workout.id,
                                            exercise.id,
                                            index,
                                            set.reps - 1,
                                            set.weight
                                        )
                                    }
                                )

                                // Reps value
                                Text(
                                    text = set.reps.toString(),
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Reps +
                                PlusMinusButton(
                                    symbol = "+",
                                    onClick = {
                                        viewModel.updateSet(
                                            workout.id,
                                            exercise.id,
                                            index,
                                            set.reps + 1,
                                            set.weight
                                        )
                                    }
                                )


                                // Weight -
                                PlusMinusButton(
                                    symbol = "-",
                                    onClick = {
                                        viewModel.updateSet(
                                            workout.id,
                                            exercise.id,
                                            index,
                                            set.reps ,
                                            set.weight - 2.5f
                                        )
                                    }
                                )


                                // Weight value
                                Text(
                                    text = "${set.weight}",
                                    modifier = Modifier.width(56.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Weight +
                                PlusMinusButton(
                                    symbol = "+",
                                    onClick = {
                                        viewModel.updateSet(
                                            workout.id,
                                            exercise.id,
                                            index,
                                            set.reps ,
                                            set.weight + 2.5f
                                        )
                                    }
                                )

                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = {
                                        viewModel.deleteSet(
                                            workout.id,
                                            exercise.id,
                                            index
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete set"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rest",
                                    modifier = Modifier.width(56.dp)
                                )
                                PlusMinusButton(
                                    symbol = "-",
                                    onClick = {
                                        viewModel.updateRest(
                                            workout.id,
                                            exercise.id,
                                            index,
                                            set.restSeconds - 15
                                        )
                                    }
                                )
                                Text(
                                    text = formatRest(set.restSeconds),
                                    modifier = Modifier.width(72.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                PlusMinusButton(
                                    symbol = "+",
                                    onClick = {
                                        viewModel.updateRest(
                                            workout.id,
                                            exercise.id,
                                            index,
                                            set.restSeconds + 15
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // ---- ADD SET ----


                        Button(
                            onClick = {
                                viewModel.addSet(workout.id, exercise.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add set"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Set")
                        }
                    }
                }
            }
        }
    }
}
