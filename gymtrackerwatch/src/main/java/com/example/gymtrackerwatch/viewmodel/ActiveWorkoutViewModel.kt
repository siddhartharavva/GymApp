package com.example.gymtrackerwatch.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerwatch.domain.model.*
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import com.example.gymtrackerwatch.domain.mapper.toActiveWorkout
import com.example.gymtrackerwatch.sync.dto.WorkoutTemplateDto
import com.example.gymtrackerwatch.sync.sender.WorkoutResultSender
import com.example.gymtrackerwatch.sync.store.IncomingWorkoutStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
class ActiveWorkoutViewModel : ViewModel() {
    private var hasSentResult = false

    enum class WorkoutUiState {
        EXERCISE,
        CONFIRM_REPS,
        CONFIRM_WEIGHT,
        REST,
        COMPLETE
    }

    var workoutUiState by mutableStateOf(WorkoutUiState.EXERCISE)
        private set

    fun goToExercise() {
        workoutUiState = WorkoutUiState.EXERCISE
    }

    fun goToConfirmReps() {
        workoutUiState = WorkoutUiState.CONFIRM_REPS
    }

    fun goToConfirmWeight() {
        workoutUiState = WorkoutUiState.CONFIRM_WEIGHT
    }

    fun goToRest() {
        workoutUiState = WorkoutUiState.REST
    }
    fun toCompletedWorkout(): CompletedWorkout {
        val w = workout ?: error("Workout not finished")

        return CompletedWorkout(
            workoutId = w.workoutId,
            name = w.name,
            startedAtEpochMs = w.startedAtEpochMs,
            completedAtEpochMs = w.completedAtEpochMs
                ?: System.currentTimeMillis(),
            exercises = w.exercises.map { ex ->
                CompletedExercise(
                    name = ex.name,
                    sets = ex.sets.mapNotNull { set ->
                        val reps = set.completedReps
                        val weight = set.completedWeight
                        val rest = set.actualRestSeconds
                        val completedAt = set.completedAtEpochMs

                        if (
                            reps != null &&
                            weight != null &&
                            rest != null &&
                            completedAt != null
                        ) {
                            CompletedSet(
                                reps = reps,
                                weight = weight,
                                actualRestSeconds = rest,
                                skippedRest = set.skippedRest,
                                completedAtEpochMs = completedAt
                            )
                        } else {
                            null
                        }
                    }
                )
            }
        )
    }
    fun sendWorkoutAndReset(context: Context) {
        val w = workout ?: return  // 🔒 hard guard

        val completed = toCompletedWorkout()
        WorkoutResultSender.send(context, completed)

        workout = null
        workoutLoaded = false
        workoutUiState = WorkoutUiState.EXERCISE
        _hasWorkout.value = false
    }

    // ---- CORE STATE ----
    var workout by mutableStateOf<ActiveWorkout?>(null)
        private set

    private var workoutLoaded = false

    private val _hasWorkout = MutableStateFlow(false)
    val hasWorkout: StateFlow<Boolean> = _hasWorkout

    // ---- DERIVED STATE ----
    val isWorkoutCompleted: Boolean
        get() = workout?.completedAtEpochMs != null

    init {
        viewModelScope.launch {
            IncomingWorkoutStore.hasWorkout
                .filter { it }
                .collect {
                    // defer to next frame
                    kotlinx.coroutines.yield()
                    loadWorkout()
                }
        }
    }

    // ---- HELPERS ----
    fun currentExercise(): ActiveExercise =
        requireNotNull(workout).exercises[workout!!.currentExerciseIndex]

    fun currentSet(): ActiveSet =
        currentExercise().sets[currentExercise().currentSetIndex]

    // ---- WORKFLOW ----
    fun confirmSet(reps: Int, weight: Float) {
        val w = requireNotNull(workout)

        val exIndex = w.currentExerciseIndex
        val setIndex = currentExercise().currentSetIndex

        val updatedSet = currentSet().copy(
            completedReps = reps,
            completedWeight = weight,
            completedAtEpochMs = System.currentTimeMillis()
        )

        val updatedSets = currentExercise().sets.toMutableList().apply {
            this[setIndex] = updatedSet
        }

        val updatedExercise = currentExercise().copy(sets = updatedSets)

        val updatedExercises = w.exercises.toMutableList().apply {
            this[exIndex] = updatedExercise
        }

        workout = w.copy(exercises = updatedExercises)
    }

    // ---- REST ----
    var restRemainingSeconds by mutableIntStateOf(0)
        private set

    var isRestRunning by mutableStateOf(false)
        private set

    private var restJob: Job? = null

    fun startRest() {
        restRemainingSeconds = currentSet().plannedRestSeconds
        isRestRunning = true

        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (restRemainingSeconds > 0) {
                delay(1000)
                restRemainingSeconds--
            }
            isRestRunning = false
            finishRestNormally()
        }
    }

    fun skipRest() {
        val elapsed = currentSet().plannedRestSeconds - restRemainingSeconds
        restJob?.cancel()
        isRestRunning = false
        restRemainingSeconds = 0
        advanceAfterRest(elapsed, skipped = true)
    }

    private fun finishRestNormally() {
        val elapsed = currentSet().plannedRestSeconds
        advanceAfterRest(elapsed, skipped = false)
    }

    // ---- CORE PROGRESSION ----
    private fun advanceAfterRest(actualRest: Int, skipped: Boolean) {
        val w = requireNotNull(workout)

        val exIndex = w.currentExerciseIndex
        val setIndex = currentExercise().currentSetIndex

        val updatedSet = currentSet().copy(
            actualRestSeconds = actualRest,
            skippedRest = skipped
        )

        val updatedSets = currentExercise().sets.toMutableList().apply {
            this[setIndex] = updatedSet
        }

        val nextSetIndex = setIndex + 1
        val isExerciseDone = nextSetIndex >= updatedSets.size

        val updatedExercise = currentExercise().copy(
            sets = updatedSets,
            currentSetIndex = if (isExerciseDone) 0 else nextSetIndex
        )

        val updatedExercises = w.exercises.toMutableList().apply {
            this[exIndex] = updatedExercise
        }

        workout =
            if (isExerciseDone && exIndex + 1 >= w.exercises.size) {
                // ✅ WORKOUT FINISHED
                workoutUiState = WorkoutUiState.COMPLETE

                w.copy(
                    exercises = updatedExercises,
                    completedAtEpochMs = System.currentTimeMillis(),
                    pendingSync = true
                )
            } else if (isExerciseDone) {
                w.copy(
                    exercises = updatedExercises,
                    currentExerciseIndex = exIndex + 1
                )
            } else {
                w.copy(exercises = updatedExercises)
            }
    }


    // ---- LOADING ----
    fun loadWorkout() {
        if (workoutLoaded || workout != null) return

        val template = IncomingWorkoutStore.consume() ?: return

        workout = template.toActiveWorkout()
        workoutLoaded = true
        workoutUiState = WorkoutUiState.EXERCISE
        _hasWorkout.value = true
    }
}