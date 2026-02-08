package com.example.gymtrackerphone.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.gymtrackerphone.data.model.CompletedSetUi
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PastWorkoutsScreen(
    viewModel: WorkoutViewModel
) {
    val workouts by viewModel.pastWorkouts.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importCompletedCsv(context, uri)
        }
    }

    val workoutFilters =
        remember(workouts) {
            workouts
                .map { it.name }
                .distinct()
                .sorted()
        }

    val filteredWorkouts =
        workouts.filter { workout ->
            selectedFilter == null || workout.name == selectedFilter
        }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (filteredWorkouts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (workouts.isEmpty()) "No workouts yet" else "No matches",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 140.dp // room for filter bar + nav + scroll overlap
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredWorkouts, key = { it.id }) { workout ->
                    val dateText = formatDateOnly(workout.completedAtEpochMs)
                    val timeText = formatTimeOnly(workout.completedAtEpochMs)
                    val durationText =
                        formatDuration(
                            workout.startedAtEpochMs,
                            workout.completedAtEpochMs
                        )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = workout.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = "$durationText • ${workout.exercises.size} exercises",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = dateText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            workout.exercises.forEach { ex ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val setScroll = rememberScrollState()
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(setScroll)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ex.sets.forEachIndexed { index, set ->
                                            SetChip(
                                                index = index + 1,
                                                set = set
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (workoutFilters.isNotEmpty()) {
            val scrollState = rememberScrollState()

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        workoutFilters.forEach { name ->
                            FilterChip(
                                selected = selectedFilter == name,
                                onClick = { selectedFilter = name },
                                label = { Text(name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { importLauncher.launch("text/*") }
                        ) {
                            Text("Import CSV")
                        }

                        TextButton(
                            onClick = {
                                exportCsv(
                                    context = context,
                                    workouts = filteredWorkouts,
                                    filterName = selectedFilter ?: "All"
                                )
                            }
                        ) {
                            Text("Export CSV")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateOnly(epochMs: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(Date(epochMs))
}

private fun formatTimeOnly(epochMs: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(epochMs))
}

private fun formatWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else weight.toString()

private fun formatDuration(startedAt: Long, completedAt: Long): String {
    val mins = ((completedAt - startedAt) / 60000L).coerceAtLeast(1)
    return "${mins}m"
}

private fun exportCsv(
    context: android.content.Context,
    workouts: List<com.example.gymtrackerphone.data.model.CompletedWorkoutUi>,
    filterName: String
) {
    if (workouts.isEmpty()) return

    val header =
        "Workout,Completed At,Duration (min),Exercise,Set,Reps,Weight\n"

    val rows = buildString {
        append(header)
        workouts.forEach { workout ->
            val completedAt = formatDateTime(workout.completedAtEpochMs)
            val duration = ((workout.completedAtEpochMs - workout.startedAtEpochMs) / 60000L)
                .coerceAtLeast(1)

            workout.exercises.forEach { ex ->
                ex.sets.forEachIndexed { index, set ->
                    append(
                        "\"${workout.name}\"," +
                            "\"$completedAt\"," +
                            "\"$duration\"," +
                            "\"${ex.name}\"," +
                            "\"${index + 1}\"," +
                            "\"${set.reps}\"," +
                            "\"${formatWeight(set.weight)}\"\n"
                    )
                }
            }
        }
    }

    val fileName =
        "gymtracker_${filterName.lowercase(Locale.getDefault())}_" +
            "${System.currentTimeMillis()}.csv"

    val file = File(context.cacheDir, fileName)
    file.writeText(rows)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(shareIntent, "Export workouts")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun formatDateTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return formatter.format(Date(epochMs))
}

@Composable
private fun SetChip(
    index: Int,
    set: CompletedSetUi
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set $index",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${set.reps}x${formatWeight(set.weight)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
