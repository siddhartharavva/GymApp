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
fun ExerciseScreen(
    vm: ActiveWorkoutViewModel
) {
    val ex = vm.currentExercise()
    val set = vm.currentSet()

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.title2
                )

                Text(
                    text = "Set ${ex.currentSetIndex + 1} / ${ex.sets.size}",
                    style = MaterialTheme.typography.caption1
                )

                Text(
                    text = "${set.targetMaxReps} reps",
                    style = MaterialTheme.typography.display2
                )

                Text(
                    text = "${set.targetWeight} kg",
                    style = MaterialTheme.typography.title3
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        vm.goToConfirmReps()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("COMPLETE SET")
                }
            }
        }
    }
}