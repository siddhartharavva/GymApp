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
    val workouts by viewModel.workouts.collectAsState()
    val workout = workouts.firstOrNull { it.id == workoutId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout") }
            )
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
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            items(workout.exercises) { exercise ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        // ---- EXERCISE HEADER ----
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.titleLarge
                            )

                            IconButton(
                                onClick = {
                                    viewModel.deleteExercise(exercise.id)
                                }
                            ) {
                                Icon(Icons.Outlined.Delete, null)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // ---- SETS ----
                        exercise.sets.forEach { set ->

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Set")

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteSet(set.id)
                                            }
                                        ) {
                                            Icon(Icons.Outlined.Delete, null)
                                        }
                                    }

                                    Text("Reps Range")

                                    RangeSlider(
                                        value = set.minReps.toFloat()..set.maxReps.toFloat(),
                                        onValueChange = {
                                            viewModel.updateRepRange(
                                                set.id,
                                                it.start.toInt(),
                                                it.endInclusive.toInt()
                                            )
                                        },
                                        valueRange = 0f..35f
                                    )

                                    Text("${set.minReps} – ${set.maxReps}")

                                    Text("Weight")

                                    WheelPicker(
                                        values = (0..200).map { (it * 2.5f).toString() },
                                        selectedIndex = (set.weight / 2.5f).toInt(),
                                        onValueChange = {
                                            viewModel.updateWeight(
                                                set.id,
                                                it * 2.5f
                                            )
                                        }
                                    )

                                    Text("Rest")

                                    WheelPicker(
                                        values = (0..30).map { "${it * 10}s" },
                                        selectedIndex = set.restSeconds / 10,
                                        onValueChange = {
                                            viewModel.updateRest(
                                                set.id,
                                                it * 10
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.addSet(exercise.id)
                            }
                        ) {
                            Icon(Icons.Outlined.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Set")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}