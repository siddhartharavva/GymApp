package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel
import com.example.gymtrackerwatch.presentation.util.formatHistorySets
import com.example.gymtrackerwatch.presentation.util.formatWeight

@Composable
fun ExerciseScreen(
    vm: ActiveWorkoutViewModel
) {
    val ex = vm.currentExercise()
    val set = vm.currentSet()
    val historyStyle = MaterialTheme.typography.caption1.copy(lineHeight = 12.sp)

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
                verticalArrangement = Arrangement.spacedBy(1.dp)
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
                    text = "${formatWeight(set.targetWeight)}kg",
                    style = MaterialTheme.typography.title3
                )

                if (ex.history.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Last 2 sessions",
                        style = historyStyle
                    )

                    ex.history.take(2).forEachIndexed { index, session ->
                        val label = if (index == 0) "Last" else "Prev"
                        Text(
                            text = "$label: ${formatHistorySets(session.sets)}",
                            style = historyStyle
                        )
                    }
                }

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
