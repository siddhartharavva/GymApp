package com.example.gymtrackerphone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt
import kotlin.math.abs

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
    itemHeight: Dp = 28.dp,
    userScrollEnabled: Boolean = true
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
        userScrollEnabled = userScrollEnabled,
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


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    workoutId: Int,
    viewModel: WorkoutViewModel
) {

    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val weightValues = remember { (0..200).map { "${it * 2.5f}" } }
    val restValues = remember { (0..30).map { "${it * 10}s" } }

    val workouts by viewModel.workouts.collectAsState()
    val workout = workouts.firstOrNull { it.id == workoutId }

    var exerciseName by remember { mutableStateOf("") }
    var exerciseToDelete by remember { mutableStateOf<DeleteExerciseRequest?>(null) }
    var setToDelete by remember { mutableStateOf<DeleteSetRequest?>(null) }
    var expandedSetId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
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
            var expandedExercises by remember { mutableStateOf(setOf<Int>()) }
            var exercises by remember(workout.exercises) { mutableStateOf(workout.exercises) }
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
                exercises = exercises.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = bottomBarHeight + 16.dp
                )
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ReorderableItem(reorderableState, key = exercise.id) { isDragging ->
                        val elevation by androidx.compose.animation.core.animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .longPressDraggableHandle(
                                    onDragStopped = {
                                        viewModel.reorderExercises(exercises.map { it.id })
                                    }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { 
                                            expandedExercises = if (expandedExercises.contains(exercise.id)) {
                                                expandedExercises - exercise.id
                                            } else {
                                                expandedExercises + exercise.id
                                            }
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
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

                                if (expandedExercises.contains(exercise.id)) {
                                    exercise.sets.forEachIndexed { index, set ->

                                        var localRange by remember(
                                            set.id,
                                            set.minReps,
                                            set.maxReps
                                        ) {
                                            mutableStateOf(
                                                set.minReps.toFloat()..set.maxReps.toFloat()
                                            )
                                        }

                                        var allowSlider by remember(set.id) { mutableStateOf(true) }

                                        LaunchedEffect(set.minReps, set.maxReps) {
                                            localRange =
                                                set.minReps.toFloat()..set.maxReps.toFloat()
                                        }

                                        val isExpanded = expandedSetId == set.id

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth().
                                                padding(vertical = 2.dp)
                                                .animateContentSize(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            ) {

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "Set ${index + 1}",
                                                        modifier = Modifier
                                                            .padding(start = 4.dp)
                                                            .clickable {
                                                                expandedSetId =
                                                                    if (isExpanded) null else set.id
                                                            }
                                                    )

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
                                                }

                                                if (isExpanded) {
                                                    val isListScrolling by remember(listState) {
                                                        derivedStateOf { listState.isScrollInProgress }
                                                    }
                                                    Text(
                                                        "Reps Range",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(28.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        RangeSlider(
                                                            value = localRange,
                                                            onValueChange = {
                                                                if (!isListScrolling && allowSlider) {
                                                                    localRange = it
                                                                }
                                                            },
                                                            onValueChangeFinished = {
                                                                val min =
                                                                    localRange.start.roundToInt()
                                                                        .coerceAtLeast(1)
                                                                val max =
                                                                    localRange.endInclusive.roundToInt()
                                                                        .coerceAtLeast(min)
                                                                        .coerceAtMost(35)
                                                                if (min != set.minReps || max != set.maxReps) {
                                                                    viewModel.updateRepRange(set.id, min, max)
                                                                }
                                                            },
                                                            modifier = Modifier.pointerInput(set.id) {
                                                                detectDragGestures(
                                                                    onDragStart = { allowSlider = true },
                                                                    onDragEnd = { allowSlider = true },
                                                                    onDragCancel = { allowSlider = true },
                                                                    onDrag = { change, dragAmount ->
                                                                        if (abs(dragAmount.y) > abs(dragAmount.x) && abs(dragAmount.y) > 6f) {
                                                                            allowSlider = false
                                                                            return@detectDragGestures
                                                                        }
                                                                        change.consume()
                                                                    }
                                                                )
                                                            },
                                                            valueRange = 1f..35f,
                                                            steps = 33,
                                                            enabled = !isListScrolling,
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
                                                            text = "${localRange.start.roundToInt()} – " +
                                                                "${localRange.endInclusive.roundToInt()} Reps",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    Text(
                                                        "Weight",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    WheelPicker(
                                                        values = weightValues,
                                                        selectedIndex = (set.weight / 2.5f).roundToInt(),
                                                        onValueChange = { weightIndex ->
                                                            val newWeight = weightIndex * 2.5f
                                                            if (newWeight != set.weight) {
                                                                viewModel.updateWeight(set.id, newWeight)
                                                            }
                                                        },
                                                        userScrollEnabled = !isListScrolling
                                                    )

                                                    Text(
                                                        "Rest",
                                                        style = MaterialTheme.typography.labelMedium
                                                    )
                                                    WheelPicker(
                                                        values = restValues,
                                                        selectedIndex = (set.restSeconds / 10),
                                                        onValueChange = { restIndex ->
                                                            val newRest = restIndex * 10
                                                            if (newRest != set.restSeconds) {
                                                                viewModel.updateRest(set.id, newRest)
                                                            }
                                                        },
                                                        userScrollEnabled = !isListScrolling
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Reps ${set.minReps} – ${set.maxReps}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                    )
                                                    Text(
                                                        text = "Weight ${set.weight} kg",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                    )
                                                    Text(
                                                        text = "Rest ${set.restSeconds}s",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
                                                    )
                                                }
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
                        }
                    }
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
                tonalElevation = 3.dp,
                color = Color(0xFF050505)
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
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2A2A2A),
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            containerColor = Color(0xFF111111)
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (exerciseName.isBlank()) return@Button
                            viewModel.addExercise(workout.id, exerciseName)
                            exerciseName = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
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
