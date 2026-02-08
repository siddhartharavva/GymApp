package com.example.gymtrackerphone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DeleteExerciseRequest(
    val id: Int,
    val name: String
)

private data class DeleteSetRequest(
    val id: Int,
    val label: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 28.dp
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)

    var currentIndex by remember { mutableStateOf(selectedIndex) }
    var committedIndex by remember { mutableStateOf(selectedIndex) }

    // 🔒 Hard lock until user scrolls
    var allowLayoutUpdates by remember { mutableStateOf(false) }
    var userScrolled by remember { mutableStateOf(false) }

    // 🔒 DB → UI sync (ABSOLUTE SOURCE OF TRUTH)
    LaunchedEffect(selectedIndex) {
        allowLayoutUpdates = false
        userScrolled = false

        currentIndex = selectedIndex
        committedIndex = selectedIndex

        listState.scrollToItem(selectedIndex)
    }

    // 🔵 Track center item (UI ONLY, gated)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layout ->
                if (!allowLayoutUpdates) return@collect

                val center =
                    (layout.viewportStartOffset + layout.viewportEndOffset) / 2

                val centeredItem = layout.visibleItemsInfo.minByOrNull { item ->
                    kotlin.math.abs((item.offset + item.size / 2) - center)
                }

                centeredItem?.let {
                    currentIndex = it.index
                }
            }
    }

    // 🟢 Commit ONLY after genuine user scroll
    LaunchedEffect(listState.isScrollInProgress) {

        if (listState.isScrollInProgress) {
            userScrolled = true
            allowLayoutUpdates = true
        }

        if (!listState.isScrollInProgress) {
            if (userScrolled && currentIndex != committedIndex) {
                committedIndex = currentIndex
                onValueChange(currentIndex)
            }
            userScrolled = false
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val itemWidth = 44.dp

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(
            horizontal = screenWidth / 2 - itemWidth / 2
        )
    ) {
        itemsIndexed(values) { index, value ->
            Box(
                modifier = Modifier.width(itemWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style =
                    if (index == currentIndex)
                        MaterialTheme.typography.titleMedium
                    else
                        MaterialTheme.typography.bodySmall,
                    color =
                    if (index == currentIndex)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
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

    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    val workouts by viewModel.workouts.collectAsState()
    val workout = workouts.firstOrNull { it.id == workoutId }

    var exerciseName by remember { mutableStateOf("") }
    var exerciseToDelete by remember { mutableStateOf<DeleteExerciseRequest?>(null) }
    var setToDelete by remember { mutableStateOf<DeleteSetRequest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout")}
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // -------- MAIN CONTENT --------
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = bottomBarHeight + 16.dp
                )

            ) {
                items(workout.exercises) { exercise ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    exercise.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.1f
                                    ),
                                    modifier = Modifier.padding(top=4.dp,start = 4.dp)
                                )

                                IconButton(
                                    onClick = {
                                        exerciseToDelete = DeleteExerciseRequest(
                                            id = exercise.id,
                                            name = exercise.name
                                        )
                                    }
                                ) {
                                    Icon(Icons.Outlined.Delete, null)
                                }
                            }


                            exercise.sets.forEachIndexed {index, set ->

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth().
                                        padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Set ${index + 1}")

                                            IconButton(
                                                onClick = {
                                                    setToDelete = DeleteSetRequest(
                                                        id = set.id,
                                                        label = "Set ${index + 1} • ${exercise.name}"
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(36.dp) // ⬅️ smaller tap area
                                                    .offset(y = (-2).dp) // ⬅️ nudges it up
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                           /* IconButton(
                                                onClick = { viewModel.deleteSet(set.id) }
                                            ) {
                                                Icon(Icons.Outlined.Delete, null)
                                            }*/
                                        }

                                        Text("Reps Range",style = MaterialTheme.typography.labelMedium)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp), // ⬅️ key line
                                            contentAlignment = Alignment.Center
                                        ) {
                                        RangeSlider(
                                            value = set.minReps.toFloat()..set.maxReps.toFloat(),
                                            onValueChange = {
                                                viewModel.updateRepRange(
                                                    set.id,
                                                    it.start.toInt(),
                                                    it.endInclusive.toInt()
                                                )
                                            },
                                            valueRange = 1f..35f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }

                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${set.minReps} – ${set.maxReps} Reps",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Text("Weight",    style = MaterialTheme.typography.labelMedium)
                                        WheelPicker(
                                            values = (0..200).map { "${it * 2.5f}" },
                                            selectedIndex = (set.weight / 2.5f).roundToInt(),
                                            onValueChange = { index ->
                                                val newWeight = index * 2.5f
                                                if (newWeight != set.weight) {
                                                    viewModel.updateWeight(set.id, newWeight)
                                                }
                                            }
                                        )


                                        Text("Rest",style = MaterialTheme.typography.labelMedium)
                                        WheelPicker(
                                            values = (0..30).map { "${it * 10}s" },
                                            selectedIndex = (set.restSeconds / 10),
                                            onValueChange = { index ->
                                                val newRest = index * 10
                                                if (newRest != set.restSeconds) {
                                                    viewModel.updateRest(set.id, newRest)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.addSet(exercise.id) }
                            ) {
                                Icon(Icons.Outlined.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Set")
                            }
                        }

                    }
                    Spacer(Modifier.height(8.dp))


                }
            }


            // -------- INPUT BAR --------
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged {
                        with(density) {
                            bottomBarHeight = it.height.toDp()
                        }
                    }
                    .imePadding(),

                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        label = { Text("Exercise name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(6.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (exerciseName.isBlank()) return@Button
                            viewModel.addExercise(workout.id, exerciseName)
                            exerciseName = ""
                        }
                    ) {
                        Text("Add Exercise")
                    }
                }
            }
        }
    }

    if (exerciseToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Delete exercise?") },
            text = {
                Text(
                    "This will remove ${exerciseToDelete?.name} and its sets. " +
                        "Past history for this exercise will no longer match."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = exerciseToDelete?.id
                        if (id != null) {
                            viewModel.deleteExercise(id)
                        }
                        exerciseToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (setToDelete != null) {
        AlertDialog(
            onDismissRequest = { setToDelete = null },
            title = { Text("Delete set?") },
            text = {
                Text(
                    "Delete ${setToDelete?.label}? This may change how past " +
                        "workouts are interpreted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = setToDelete?.id
                        if (id != null) {
                            viewModel.deleteSet(id)
                        }
                        setToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { setToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
