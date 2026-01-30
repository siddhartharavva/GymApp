package com.example.gymtrackerphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import com.example.gymtrackerphone.data.Workout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onWorkoutClick: (Workout) -> Unit
){

    var textInput by remember { mutableStateOf("") }
    var editingWorkoutId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = Modifier.imePadding(),

        topBar = {
            TopAppBar(
                title = { Text("My Workouts") }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Workout name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(viewModel.workouts) { workout ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            onWorkoutClick(workout)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Workout name (NOT clickable anymore)
                        Text(
                            text = workout.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )




                        Row {
                            IconButton(
                                onClick = {
                                    textInput = workout.name
                                    editingWorkoutId = workout.id
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit workout",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteWorkout(workout.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete workout",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}