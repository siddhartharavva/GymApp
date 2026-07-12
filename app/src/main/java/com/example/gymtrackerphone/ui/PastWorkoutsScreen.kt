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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.gymtrackerphone.data.model.CompletedSetUi
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastWorkoutsScreen(
    viewModel: WorkoutViewModel
) {
    val workouts by viewModel.pastWorkouts.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var editSet by remember { mutableStateOf<EditSetRequest?>(null) }
    var editExerciseName by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var expandedWorkoutId by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
        remember(workouts, selectedFilter) {
            workouts.filter { workout ->
                selectedFilter == null || workout.name == selectedFilter
            }
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
                    val dateText =
                        remember(workout.completedAtEpochMs) {
                            formatDateOnly(workout.completedAtEpochMs)
                        }
                    val timeText =
                        remember(workout.completedAtEpochMs) {
                            formatTimeOnly(workout.completedAtEpochMs)
                        }
                    val durationText =
                        remember(
                            workout.startedAtEpochMs,
                            workout.completedAtEpochMs
                        ) {
                            formatDuration(
                                workout.startedAtEpochMs,
                                workout.completedAtEpochMs
                            )
                        }
                    val totalSets =
                        remember(workout.id) {
                            workout.exercises.sumOf { exercise -> exercise.sets.size }
                        }
                    val isExpanded = expandedWorkoutId == workout.id

                    var lastProgress by remember { mutableStateOf(0f) }
                    var hapticFired by remember { mutableStateOf(false) }
                    val dismissState =
                        rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (
                                    value == SwipeToDismissBoxValue.StartToEnd &&
                                    lastProgress >= 0.5f
                                ) {
                                    viewModel.deleteCompletedWorkout(workout.id)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                    // Haptic at halfway swipe
                    LaunchedEffect(dismissState.progress) {
                        val progress = dismissState.progress
                        lastProgress = progress
                        if (!hapticFired && progress >= 0.5f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            hapticFired = true
                        }
                        if (progress < 0.1f) {
                            hapticFired = false
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    ) {
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$totalSets sets",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            expandedWorkoutId =
                                                if (isExpanded) null else workout.id
                                        }
                                    ) {
                                        Text(if (isExpanded) "Hide Sets" else "Show Sets")
                                    }
                                }

                                if (isExpanded) {
                                    workout.exercises.forEach { ex ->
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = ex.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    editExerciseName = ex.id to ex.name
                                                }
                                            )

                                            LazyRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                itemsIndexed(
                                                    items = ex.sets,
                                                    key = { _, set -> set.id }
                                                ) { index, set ->
                                                    SetChip(
                                                        index = index + 1,
                                                        set = set,
                                                        onClick = {
                                                            editSet = EditSetRequest(
                                                                id = set.id,
                                                                label = "${workout.name} • ${ex.name} • Set ${index + 1}",
                                                                reps = set.reps,
                                                                weight = set.weight
                                                            )
                                                        }
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
            }
        }

    if (editSet != null) {
        EditSetDialog(
            request = editSet!!,
            onDismiss = { editSet = null },
            onSave = { reps, weight ->
                viewModel.updateCompletedSet(
                    setId = editSet!!.id,
                    reps = reps,
                    weight = weight
                )
                editSet = null
            }
        )
    }

    if (editExerciseName != null) {
        EditExerciseNameDialog(
            initialName = editExerciseName!!.second,
            onDismiss = { editExerciseName = null },
            onSave = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.updateCompletedExerciseName(
                        exerciseId = editExerciseName!!.first,
                        name = newName.trim()
                    )
                }
                editExerciseName = null
            }
        )
    }


    if (workoutFilters.isNotEmpty()) {
        val scrollState = rememberScrollState()

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 3.dp,
            color = Color(0xFF050505)
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
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF111111),
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )

                    workoutFilters.forEach { name ->
                        FilterChip(
                            selected = selectedFilter == name,
                            onClick = { selectedFilter = name },
                            label = { Text(name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF111111),
                                labelColor = Color.White.copy(alpha = 0.8f)
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
                        Text("Import CSV", color = Color.White)
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
                        Text("Export CSV", color = Color.White)
                    }
                }
            }
        }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 3.dp,
                color = Color(0xFF050505)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { importLauncher.launch("text/*") }
                    ) {
                        Text("Import CSV", color = Color.White)
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
    set: CompletedSetUi,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set $index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${set.reps}x${formatWeight(set.weight)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class EditSetRequest(
    val id: Int,
    val label: String,
    val reps: Int,
    val weight: Float
)


@Composable
private fun EditSetDialog(
    request: EditSetRequest,
    onDismiss: () -> Unit,
    onSave: (Int, Float) -> Unit
) {
    var repsText by remember(request) { mutableStateOf(request.reps.toString()) }
    var weightText by remember(request) { mutableStateOf(request.weight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Set") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(request.label, style = MaterialTheme.typography.labelMedium)

                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reps = repsText.toIntOrNull() ?: request.reps
                    val weight = weightText.toFloatOrNull() ?: request.weight
                    onSave(reps.coerceAtLeast(0), weight.coerceAtLeast(0f))
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditExerciseNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Exercise Name") },
        text = {
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Exercise Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nameText) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
