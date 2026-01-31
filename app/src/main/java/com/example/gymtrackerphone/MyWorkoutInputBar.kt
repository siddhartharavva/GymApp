package com.example.gymtrackerphone

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.gymtrackerphone.ui.navigation.HomeTab
import com.example.gymtrackerphone.data.model.WorkoutUi
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel



@Composable
fun MyWorkoutInputBar(
    viewModel: WorkoutViewModel
) {
    var textInput by remember { mutableStateOf("") }
    var editingWorkoutId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding() // ✅ ONLY keyboard, no navbar
            .padding(16.dp)
    ) {

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Workout name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

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
            Text(if (editingWorkoutId == null) "Add Workout" else "Update Workout")
        }
    }
}