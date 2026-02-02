package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.runtime.Composable
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel
@Composable
fun WorkoutFlowScreen(vm: ActiveWorkoutViewModel) {
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
}