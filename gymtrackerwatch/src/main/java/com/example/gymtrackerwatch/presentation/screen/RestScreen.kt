package com.example.gymtrackerwatch.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.gymtrackerwatch.presentation.util.formatWeight
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

@Composable
fun RestScreen(
    vm: ActiveWorkoutViewModel
) {
    val restSeconds = vm.restRemainingSeconds
    val nextPreview = vm.upcomingSetPreview()

    // 🔥 Start rest ONCE when screen appears
    LaunchedEffect(Unit) {
        vm.startRest()
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

                nextPreview?.let { next ->
                    val repsText =
                        if (next.minReps == next.maxReps) {
                            "${next.maxReps} reps"
                        } else {
                            "${next.minReps}-${next.maxReps} reps"
                        }
                    Text(
                        text = "Next: ${next.exerciseName}",
                        style = MaterialTheme.typography.caption1
                    )
                    Text(
                        text = "Set ${next.setNumber}/${next.totalSets} • $repsText • ${formatWeight(next.weight)}kg",
                        style = MaterialTheme.typography.caption2
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(0.75f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { vm.undoRest() },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("UNDO")
                    }
                    Button(
                        onClick = { vm.skipRest() },
                        colors = ButtonDefaults.primaryButtonColors(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("SKIP")
                    }
                }
            }
        }
    }
}
