package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
fun AddCustomExerciseRestScreen(
    vm: ActiveWorkoutViewModel
) {
    val rest = vm.pendingCustomRest

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "REST (sec)",
                    style = MaterialTheme.typography.caption1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.updatePendingCustomRest(rest - 10) },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("-")
                    }

                    Text(
                        text = "${rest}s",
                        style = MaterialTheme.typography.display1
                    )

                    Button(
                        onClick = { vm.updatePendingCustomRest(rest + 10) },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("+")
                    }
                }

                Button(
                    onClick = { vm.confirmCustomExercise() },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("START")
                }

            }
        }
    }
}
