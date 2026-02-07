package com.example.gymtrackerwatch.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel
@Composable
fun WorkoutFlowScreen(vm: ActiveWorkoutViewModel) {
    if (vm.workout == null) {
        IdleScreen()
        return
    }

    var showEndConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = vm.workoutUiState != ActiveWorkoutViewModel.WorkoutUiState.COMPLETE) {
        showEndConfirm = true
    }

    when (vm.workoutUiState) {

        ActiveWorkoutViewModel.WorkoutUiState.EXERCISE ->
            ExerciseScreen(vm)

        ActiveWorkoutViewModel.WorkoutUiState.CONFIRM_REPS ->
            ConfirmRepsScreen(vm)

        ActiveWorkoutViewModel.WorkoutUiState.CONFIRM_WEIGHT ->
            ConfirmWeightScreen(vm)

        ActiveWorkoutViewModel.WorkoutUiState.REST ->
            RestScreen(vm)

        ActiveWorkoutViewModel.WorkoutUiState.COMPLETE ->
            WorkoutCompleteScreen(vm)
    }

    if (showEndConfirm) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "End workout?",
                    style = MaterialTheme.typography.title2
                )

                Button(
                    onClick = { showEndConfirm = false },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(44.dp)
                ) {
                    Text("Continue")
                }

                Button(
                    onClick = {
                        showEndConfirm = false
                        vm.endWorkoutEarly()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(44.dp)
                ) {
                    Text("End & Send")
                }
            }
        }
    }
}
