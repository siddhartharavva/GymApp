package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel


@Composable
fun ConfirmWeightScreen(
    vm: ActiveWorkoutViewModel
) {
    val weight = vm.currentSet().completedWeight
        ?: vm.currentSet().targetWeight

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
                    text = "WEIGHT",
                    style = MaterialTheme.typography.caption1
                )

                Text(
                    text = "$weight kg",
                    style = MaterialTheme.typography.display1
                )

                Button(
                    onClick = {
                        vm.confirmSet(
                            vm.currentSet().completedReps
                                ?: vm.currentSet().targetMaxReps,
                            weight
                        )
                        vm.goToRest() // 🔥 THIS is the missing piece
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("CONFIRM")
                }
            }
        }
    }
}