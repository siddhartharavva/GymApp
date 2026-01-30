package com.example.gymtrackerphone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import kotlin.math.abs


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp // Increased height for better touch targets
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex).coerceAtLeast(0)
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Track the internal center to update UI color immediately
    var centeredIndex by remember { mutableStateOf(selectedIndex) }

    // OPTIMIZATION: Use snapshotFlow to observe scroll changes efficiently
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val centerItem = layoutInfo.visibleItemsInfo
                    .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }

                centerItem?.let {
                    val adjustedIndex = it.index.coerceIn(0, values.lastIndex)
                    if (centeredIndex != adjustedIndex) {
                        centeredIndex = adjustedIndex
                        onValueChange(adjustedIndex)
                    }
                }
            }
    }

    LazyRow(
        state = listState,
        flingBehavior = snapFlingBehavior,
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = (LocalConfiguration.current.screenWidthDp.dp / 2) - 32.dp)
    ) {
        // Removed manual Spacers and replaced with contentPadding for cleaner code
        itemsIndexed(values) { index, value ->
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = if (index == centeredIndex)
                        MaterialTheme.typography.titleLarge
                    else
                        MaterialTheme.typography.bodyMedium,
                    color = if (index == centeredIndex)
                        MaterialTheme.colorScheme.primary // The light blue in your photo
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    workoutId: Int,
    viewModel: WorkoutViewModel
) {
    val workout = viewModel.getWorkoutById(workoutId)

    Scaffold(
        modifier = Modifier.imePadding(),

        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout") }
            )
        },
        bottomBar = {
            var newExerciseName by remember { mutableStateOf("") }

            Card(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        label = { Text("New Exercise") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (newExerciseName.isNotBlank() && workout != null) {
                                viewModel.addExercise(workout.id, newExerciseName)
                                newExerciseName = ""
                            }
                        }
                    ) {
                        Text("Add Exercise")
                    }
                }
            }
        }
    ) { paddingValues ->

        if (workout == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Workout not found")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = paddingValues.calculateBottomPadding() + 24.dp,
                    top = paddingValues.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            items(workout.exercises) { exercise ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        // ---- EXERCISE HEADER ----
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(
                                onClick = {
                                    viewModel.deleteExercise(workout.id, exercise.id)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete exercise"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // ---- SETS ----
                        exercise.sets.forEachIndexed { index, set ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {

                                    // ---- SET HEADER ----
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Set ${index + 1}",
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        IconButton(
                                            modifier = Modifier.size(48.dp),
                                            onClick = {
                                                viewModel.deleteSet(
                                                    workout.id,
                                                    exercise.id,
                                                    index
                                                )
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Delete set",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        "Reps Range",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    RangeSlider(
                                        value = set.minReps.toFloat()..set.maxReps.toFloat(),
                                        onValueChange = { range ->
                                            viewModel.updateRepRange(
                                                workout.id,
                                                exercise.id,
                                                index,
                                                range.start.toInt(),
                                                range.endInclusive.toInt()
                                            )
                                        },
                                        valueRange = 0f..35f,
                                        steps = 24,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                                            thumbColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    Text(
                                        text = "${set.minReps} – ${set.maxReps} reps",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // ---- WEIGHT ----
                                    Text(
                                        "Weight",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val weightValues = remember {
                                        (0..200).map { (it * 2.5f).toString() }
                                    }

                                    WheelPicker(
                                        values = weightValues,
                                        selectedIndex = (set.weight / 2.5f).toInt(),
                                        onValueChange = { pickerIndex ->
                                            viewModel.updateSet(
                                                workout.id,
                                                exercise.id,
                                                index,
                                                pickerIndex * 2.5f
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // ---- REST ----
                                    Text(
                                        "Rest",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val restValues = remember {
                                        (0..30).map { "${it * 10}s" }
                                    }

                                    WheelPicker(
                                        values = restValues,
                                        selectedIndex = set.restSeconds / 10,
                                        onValueChange = { pickerIndex ->
                                            viewModel.updateRest(
                                                workout.id,
                                                exercise.id,
                                                index,
                                                pickerIndex * 10
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))

                        // ---- ADD SET ----
                        Button(
                            onClick = {
                                viewModel.addSet(workout.id, exercise.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add set"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Set")
                        }
                    }
                }
            }
        }
    }
}
