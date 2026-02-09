package com.example.gymtrackerwatch.viewmodel

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.example.gymtrackerwatch.sync.sender.WorkoutResultSender
import com.example.gymtrackerwatch.sync.store.IncomingWorkoutStore
import com.example.gymtrackerwatch.sync.store.WorkoutAckStore
import com.example.gymtrackerwatch.sync.store.PendingWorkoutStore
import com.example.gymtrackerwatch.sync.store.ActiveWorkoutStore
import com.example.gymtrackerwatch.sync.store.ActiveWorkoutState
import com.example.gymtrackerwatch.util.RestAlarmScheduler
import com.example.gymtrackerwatch.util.RestHapticStore
import com.example.gymtrackerwatch.notifications.WatchNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
class ActiveWorkoutViewModel : ViewModel() {
    private var waitingForAck by mutableStateOf(false)
    private var rotaryAccum = 0f
    private var pendingWorkout: CompletedWorkout? = null
    private var retryJob: Job? = null
    private var ackWatchJob: Job? = null
    private var appContext: Context? = null
    private var restEndElapsedMs: Long = 0L
    private var started by mutableStateOf(false)
    val isWaitingForAck: Boolean
        get() = waitingForAck
    val isStarted: Boolean
        get() = started

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

        val completedExercises =
            w.exercises.mapNotNull { ex ->
                val completedSets =
                    ex.sets.mapIndexedNotNull { setIndex, set ->
                        val reps = set.completedReps
                        val weight = set.completedWeight
                        val rest = set.actualRestSeconds ?: 0
                        val completedAt = set.completedAtEpochMs

                        if (
                            reps != null &&
                            weight != null &&
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

                if (completedSets.isEmpty()) null
                else CompletedExercise(name = ex.name, sets = completedSets)
            }

        return CompletedWorkout(
            workoutId = w.workoutId,
            name = w.name,
            startedAtEpochMs = w.startedAtEpochMs,
            completedAtEpochMs = w.completedAtEpochMs
                ?: System.currentTimeMillis(),
            exercises = completedExercises
        )
    }
    fun sendWorkoutAndReset(context: Context) {
        if (waitingForAck) return
        workout ?: return  // hard guard
        appContext = context.applicationContext

        val completed = toCompletedWorkout()
        if (completed.exercises.isEmpty()) {
            // No completed sets to sync; just clear local finished workout.
            workout = null
            workoutLoaded = false
            workoutUiState = WorkoutUiState.EXERCISE
            _hasWorkout.value = false
            started = false
            restEndElapsedMs = 0L
            appContext?.let {
                ActiveWorkoutStore.clear(it)
                RestAlarmScheduler.cancel(it)
                RestHapticStore.clear(it)
            }
            return
        }
        waitingForAck = true
        pendingWorkout = completed
        PendingWorkoutStore.save(context, completed)
        sendCompleted(context, completed)
        startRetryLoop(context)
        startAckWatchdog(context)
    }

    fun tryResendPending(context: Context) {
        if (waitingForAck) return
        appContext = context.applicationContext
        val pending = PendingWorkoutStore.load(context) ?: return
        pendingWorkout = pending
        waitingForAck = true
        sendCompleted(context, pending)
        startRetryLoop(context)
        startAckWatchdog(context)
    }

    fun attachContext(context: Context) {
        appContext = context.applicationContext
        restoreFromStore(appContext ?: return)
    }

    fun markStarted() {
        started = true
        persistState()
    }

    fun endWorkoutEarly() {
        val w = workout ?: return
        if (w.completedAtEpochMs != null) return

        workoutUiState = WorkoutUiState.COMPLETE
        workout = w.copy(
            completedAtEpochMs = System.currentTimeMillis(),
            pendingSync = true
        )
        persistState()
    }

    fun cancelWorkout() {
        workout = null
        workoutLoaded = false
        workoutUiState = WorkoutUiState.EXERCISE
        _hasWorkout.value = false
        waitingForAck = false
        pendingWorkout = null
        started = false
        restEndElapsedMs = 0L
        retryJob?.cancel()
        ackWatchJob?.cancel()
        appContext?.let { ctx ->
            PendingWorkoutStore.clear(ctx)
            ActiveWorkoutStore.clear(ctx)
            IncomingWorkoutStore.clear()
            RestAlarmScheduler.cancel(ctx)
            RestHapticStore.clear(ctx)
        }
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
        persistState()
    }

    // ---- REST ----
    var restRemainingSeconds by mutableIntStateOf(0)
        private set

    var isRestRunning by mutableStateOf(false)
        private set

    private var restJob: Job? = null

    fun startRest() {
        if (!isRestRunning || restEndElapsedMs <= 0L) {
            val planned = currentSet().plannedRestSeconds
            restEndElapsedMs = SystemClock.elapsedRealtime() + planned * 1000L
            restRemainingSeconds = planned
            isRestRunning = true
            persistState()
            appContext?.let {
                RestAlarmScheduler.schedule(it, restEndElapsedMs, currentRestToken())
            }
        }
        syncRestFromClock()
        startRestJob()
    }

    fun skipRest() {
        val elapsed = currentSet().plannedRestSeconds - restRemainingSeconds
        restJob?.cancel()
        isRestRunning = false
        restEndElapsedMs = 0L
        restRemainingSeconds = 0
        appContext?.let { RestAlarmScheduler.cancel(it) }
        advanceAfterRest(elapsed, skipped = true)
        persistState()
    }

    private fun finishRestNormally() {
        appContext?.let { RestAlarmScheduler.cancel(it) }
        triggerRestHaptic()
        appContext?.let { WatchNotificationHelper.showRestComplete(it) }
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
        persistState()
    }

    // haptic handled by RestAlarmReceiver (works even with screen off)


    // ---- LOADING ----
    fun loadWorkout() {
        if (workoutLoaded || workout != null) return

        val template = IncomingWorkoutStore.consume() ?: return

        workout = template.toActiveWorkout()
        workoutLoaded = true
        workoutUiState = WorkoutUiState.EXERCISE
        _hasWorkout.value = true
        started = false
        restEndElapsedMs = 0L
        persistState()
    }

    private fun resetAfterAck() {
        workout = null
        workoutLoaded = false
        workoutUiState = WorkoutUiState.EXERCISE
        _hasWorkout.value = false
        waitingForAck = false
        pendingWorkout = null
        started = false
        restEndElapsedMs = 0L
        retryJob?.cancel()
        ackWatchJob?.cancel()
        appContext?.let { PendingWorkoutStore.clear(it) }
        appContext?.let { ActiveWorkoutStore.clear(it) }
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

    private fun startAckWatchdog(context: Context) {
        ackWatchJob?.cancel()
        val appCtx = context.applicationContext
        ackWatchJob = viewModelScope.launch {
            var checks = 0
            while (waitingForAck && checks < 50) { // ~10s @ 200ms
                delay(200)
                if (!PendingWorkoutStore.hasPending(appCtx)) {
                    handleAck()
                    break
                }
                checks++
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

    fun onAppVisible() {
        syncRestFromClock()
        startRestJob()
    }

    private fun remainingRestSeconds(): Int {
        val end = restEndElapsedMs
        if (end <= 0L) return 0
        val now = SystemClock.elapsedRealtime()
        return ((end - now) / 1000L).toInt().coerceAtLeast(0)
    }

    private fun syncRestFromClock() {
        if (restEndElapsedMs <= 0L) return
        val remaining = remainingRestSeconds()
        restRemainingSeconds = remaining
        isRestRunning = remaining > 0
        if (remaining <= 0) {
            restEndElapsedMs = 0L
            if (isRestRunning) {
                isRestRunning = false
            }
            appContext?.let { RestAlarmScheduler.cancel(it) }
            triggerRestHaptic()
            finishRestNormally()
            persistState()
        }
    }

    private fun currentRestToken(): String {
        val w = workout ?: return "rest_unknown"
        val exIndex = w.currentExerciseIndex
        val setIndex = currentExercise().currentSetIndex
        return "${w.workoutId}|$exIndex|$setIndex|rest"
    }

    private fun triggerRestHaptic() {
        val ctx = appContext ?: return
        val token = currentRestToken()
        if (RestHapticStore.wasFired(ctx, token)) return

        RestHapticStore.markFired(ctx, token)
        val vibrator = ctx.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createOneShot(
                800,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    private fun startRestJob() {
        if (!isRestRunning || restEndElapsedMs <= 0L) return
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val remaining = remainingRestSeconds()
                restRemainingSeconds = remaining
                if (remaining <= 0) break
            }
            isRestRunning = false
            restEndElapsedMs = 0L
            finishRestNormally()
            persistState()
        }
    }

    private fun persistState() {
        val ctx = appContext ?: return
        val w = workout ?: run {
            ActiveWorkoutStore.clear(ctx)
            return
        }

        ActiveWorkoutStore.save(
            ctx,
            ActiveWorkoutState(
                workout = w,
                uiState = workoutUiState.name,
                restEndElapsedMs = restEndElapsedMs,
                started = started
            )
        )
    }

    private fun restoreFromStore(context: Context) {
        val state = ActiveWorkoutStore.load(context) ?: return
        workout = state.workout
        workoutLoaded = true
        workoutUiState =
            runCatching { WorkoutUiState.valueOf(state.uiState) }
                .getOrElse { WorkoutUiState.EXERCISE }
        started = state.started
        restEndElapsedMs = state.restEndElapsedMs
        _hasWorkout.value = false
        syncRestFromClock()
        startRestJob()
    }
}
