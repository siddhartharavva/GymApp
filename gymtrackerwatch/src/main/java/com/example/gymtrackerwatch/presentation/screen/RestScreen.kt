package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

@Composable
fun RestScreen(
    vm: ActiveWorkoutViewModel
) {
    val restSeconds = vm.restRemainingSeconds

    // 🔥 Start rest ONCE when screen appears
    LaunchedEffect(Unit) {
        vm.startRest()
    }

    // 🔔 Auto-advance when rest finishes
    LaunchedEffect(restSeconds) {
        if (restSeconds == 0 && vm.isRestRunning.not()) {
            if (vm.isWorkoutCompleted) {
                // do nothing, global navigation will move to complete
            } else {
                vm.goToExercise()
            }
        }
    }

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

                Text("REST", style = MaterialTheme.typography.title2)

                Text(
                    text = "${restSeconds}s",
                    style = MaterialTheme.typography.display1
                )

                Button(
                    onClick = {
                        vm.skipRest()
                        if (!vm.isWorkoutCompleted) {
                            vm.goToExercise()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("SKIP")
                }
            }
        }
    }
}
