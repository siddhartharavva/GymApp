package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

@Composable
fun ConfirmRepsScreen(
    vm: ActiveWorkoutViewModel
) {
    val reps = vm.currentSet().completedReps
        ?: vm.currentSet().targetMaxReps

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.caption1
                )

                Text(
                    text = reps.toString(),
                    style = MaterialTheme.typography.display1
                )

                Button(
                    onClick = { vm.goToConfirmWeight() },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("NEXT")
                }
            }
        }
    }
}