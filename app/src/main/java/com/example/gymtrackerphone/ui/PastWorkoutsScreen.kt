package com.example.gymtrackerphone.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel

@Composable
fun PastWorkoutsScreen(
    viewModel: WorkoutViewModel
) {
    val workouts by viewModel.pastWorkouts.collectAsState()

    if (workouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No workouts yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(workouts) { workout ->
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}