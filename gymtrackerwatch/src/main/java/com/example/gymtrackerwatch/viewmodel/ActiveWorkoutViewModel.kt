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
import com.example.gymtrackerwatch.sync.store.WorkoutAckStore
import com.example.gymtrackerwatch.sync.store.PendingWorkoutStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
class ActiveWorkoutViewModel : ViewModel() {
    private var hasSentResult = false
    private var waitingForAck by mutableStateOf(false)
    private var rotaryAccum = 0f
    private var pendingWorkout: CompletedWorkout? = null
    private var retryJob: Job? = null
    private var appContext: Context? = null
    val isWaitingForAck: Boolean
        get() = waitingForAck

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
        initPendingForCurrentSet()
        workoutUiState = WorkoutUiState.CONFIRM_REPS
    }

    fun goToConfirmWeight() {
        workoutUiState = WorkoutUiState.CONFIRM_WEIGHT
    }

    fun goToRest() {
        workoutUiState = WorkoutUiState.REST
    }

    var pendingReps by mutableIntStateOf(0)
        private set

    var pendingWeight by mutableStateOf(0f)
        private set

    fun updatePendingReps(value: Int) {
        pendingReps = value.coerceAtLeast(0)
    }

    fun updatePendingWeight(value: Float) {
        pendingWeight = value.coerceAtLeast(0f)
    }

    private fun initPendingForCurrentSet() {
        val set = currentSet()
        pendingReps = set.completedReps ?: set.targetMaxReps
        pendingWeight = set.completedWeight ?: set.targetWeight
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
                    sets = ex.sets.mapIndexedNotNull { setIndex, set ->
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
                                setIndex = setIndex,
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
        if (waitingForAck) return
        val w = workout ?: return  // 🔒 hard guard
        appContext = context.applicationContext

        val completed = toCompletedWorkout()
        waitingForAck = true
        pendingWorkout = completed
        PendingWorkoutStore.save(context, completed)
        sendCompleted(context, completed)
        startRetryLoop(context)
    }

    fun tryResendPending(context: Context) {
        if (waitingForAck) return
        appContext = context.applicationContext
        val pending = PendingWorkoutStore.load(context) ?: return
        pendingWorkout = pending
        waitingForAck = true
        sendCompleted(context, pending)
        startRetryLoop(context)
    }

    fun endWorkoutEarly() {
        val w = workout ?: return
        if (w.completedAtEpochMs != null) return

        workoutUiState = WorkoutUiState.COMPLETE
        workout = w.copy(
            completedAtEpochMs = System.currentTimeMillis(),
            pendingSync = true
        )
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

        viewModelScope.launch {
            WorkoutAckStore.ackReceived
                .filter { it }
                .collect {
                    handleAck()
                    WorkoutAckStore.consume()
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

    private fun resetAfterAck() {
        workout = null
        workoutLoaded = false
        workoutUiState = WorkoutUiState.EXERCISE
        _hasWorkout.value = false
        waitingForAck = false
        pendingWorkout = null
        retryJob?.cancel()
        appContext?.let { PendingWorkoutStore.clear(it) }
        loadWorkout()
    }

    private fun handleAck() {
        val hasPending =
            appContext?.let { PendingWorkoutStore.hasPending(it) } ?: false
        if (waitingForAck || hasPending || pendingWorkout != null) {
            resetAfterAck()
        }
    }

    private fun sendCompleted(context: Context, workout: CompletedWorkout) {
        WorkoutResultSender.send(context, workout)
    }

    private fun startRetryLoop(context: Context) {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            var attempts = 0
            while (waitingForAck && attempts < 5) {
                delay(30_000)
                if (!waitingForAck) break
                pendingWorkout?.let { sendCompleted(context, it) }
                attempts++
            }
        }
    }

    fun handleRotaryDelta(delta: Float): Boolean {
        val stepPx = 1f
        when (workoutUiState) {
            WorkoutUiState.CONFIRM_REPS -> {
                rotaryAccum += delta
                val steps = (rotaryAccum / stepPx).toInt()
                if (steps != 0) {
                    rotaryAccum -= steps * stepPx
                    updatePendingReps(pendingReps + steps)
                }
                return true
            }

            WorkoutUiState.CONFIRM_WEIGHT -> {
                rotaryAccum += delta
                val steps = (rotaryAccum / stepPx).toInt()
                if (steps != 0) {
                    rotaryAccum -= steps * stepPx
                    updatePendingWeight(pendingWeight + (steps * 2.5f))
                }
                return true
            }

            else -> return false
        }
    }
}
