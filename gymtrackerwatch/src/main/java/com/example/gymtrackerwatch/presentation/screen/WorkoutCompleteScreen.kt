package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.compose.ui.platform.LocalContext
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

@Composable
fun WorkoutCompleteScreen(
    vm: ActiveWorkoutViewModel,
) {
    val context = LocalContext.current   // ✅ ADD THIS

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {

                Text(
                    text = "Workout complete 🎉",
                    style = MaterialTheme.typography.title2
                )

                Button(
                    enabled = vm.workout != null && !vm.isWaitingForAck,
                    onClick = {
                        vm.sendWorkoutAndReset(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(if (vm.isWaitingForAck) "Sending..." else "Send to Phone")
                }

                Button(
                    enabled = vm.workout != null && !vm.isWaitingForAck,
                    onClick = {
                        vm.goToAddCustomExerciseSets()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(44.dp),
                    colors = androidx.wear.compose.material.ButtonDefaults.secondaryButtonColors(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Add Exercise")
                }
            }
        }
    }
}
