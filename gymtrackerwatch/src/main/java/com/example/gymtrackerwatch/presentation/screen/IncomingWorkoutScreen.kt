package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.gymtrackerwatch.sync.store.IncomingWorkoutStore
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

@Composable
fun IncomingWorkoutScreen(
    vm: ActiveWorkoutViewModel,
    onStart: () -> Unit
) {
    LaunchedEffect(Unit) {
        vm.loadWorkout()
    }

    val workout = vm.workout ?: return

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.title2
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${workout.exercises.size} exercises",
                style = MaterialTheme.typography.caption1
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth(0.75f)   // pill width
                    .height(56.dp),        // pill height
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "START",
                    style = MaterialTheme.typography.button
                )
            }
        }
    }
}