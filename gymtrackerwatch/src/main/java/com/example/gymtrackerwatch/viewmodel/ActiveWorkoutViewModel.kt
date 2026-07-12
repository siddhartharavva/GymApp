package com.example.gymtrackerwatch.viewmodel

import android.content.Context
import android.os.SystemClock
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
import com.example.gymtrackerwatch.util.AppVisibilityStore
import com.example.gymtrackerwatch.util.HapticUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
class ActiveWorkoutViewModel : ViewModel() {
    data class UpcomingSetPreview(
        val exerciseName: String,
        val setNumber: Int,
        val totalSets: Int,
        val minReps: Int,
        val maxReps: Int,
        val weight: Float
    )

    private var waitingForAck by mutableStateOf(false)
    private var rotaryAccum = 0f
    private var pendingWorkout: CompletedWorkout? = null
    private var retryJob: Job? = null
    private var ackWatchJob: Job? = null
    private var appContext: Context? = null
    private var restEndElapsedMs: Long = 0L
    private var started by mutableStateOf(false)
    private var lastUiActionElapsedMs: Long = 0L
    private val uiActionDebounceMs = 350L
    val isWaitingForAck: Boolean
        get() = waitingForAck
    val isStarted: Boolean
        get() = started

    enum class WorkoutUiState {
        EXERCISE,
        CONFIRM_REPS,
        CONFIRM_WEIGHT,
        REST,
        COMPLETE,
        ADD_CUSTOM_EXERCISE_SETS,
        ADD_CUSTOM_EXERCISE_REST
    }

    var workoutUiState by mutableStateOf(WorkoutUiState.EXERCISE)
        private set

    fun goToExercise() {
        workoutUiState = WorkoutUiState.EXERCISE
        persistState()
    }

    fun goToConfirmReps() {
        if (workoutUiState != WorkoutUiState.EXERCISE) return
        if (!consumeUiAction()) return
        initPendingForCurrentSet()
        workoutUiState = WorkoutUiState.CONFIRM_REPS
        persistState()
    }

    fun goToConfirmWeight() {
        if (workoutUiState != WorkoutUiState.CONFIRM_REPS) return
        if (!consumeUiAction()) return
        workoutUiState = WorkoutUiState.CONFIRM_WEIGHT
        persistState()
    }

    fun goToRest() {
        if (workoutUiState != WorkoutUiState.CONFIRM_WEIGHT) return
        if (!consumeUiAction()) return
        workoutUiState = WorkoutUiState.REST
        persistState()
    }

    fun confirmCurrentSet() {
        if (workoutUiState != WorkoutUiState.CONFIRM_WEIGHT) return
        if (!consumeUiAction()) return
        confirmSet(pendingReps, pendingWeight)
        workoutUiState = WorkoutUiState.REST
        persistState()
    }

    fun undoConfirmWeight() {
        if (workoutUiState != WorkoutUiState.CONFIRM_WEIGHT) return
        if (!consumeUiAction()) return
        workoutUiState = WorkoutUiState.CONFIRM_REPS
        persistState()
    }

    fun undoRest() {
        if (workoutUiState != WorkoutUiState.REST) return
        if (!consumeUiAction()) return

        
        // Revert set completion status
        val w = requireNotNull(workout)
        val exIndex = w.currentExerciseIndex
        val setIndex = currentExercise().currentSetIndex
        
        val updatedSet = currentSet().copy(
            completedReps = null,
            completedWeight = null,
            completedAtEpochMs = null
        )
        val updatedSets = currentExercise().sets.toMutableList().apply {
            this[setIndex] = updatedSet
        }
        val updatedExercise = currentExercise().copy(sets = updatedSets)
        val updatedExercises = w.exercises.toMutableList().apply {
            this[exIndex] = updatedExercise
        }
        
        workout = w.copy(exercises = updatedExercises)
        workoutUiState = WorkoutUiState.CONFIRM_WEIGHT
        persistState()
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

    var pendingCustomSets by mutableIntStateOf(3)
        private set
    var pendingCustomRest by mutableIntStateOf(60)
        private set
    var customExerciseCount by mutableIntStateOf(1)
        private set

    fun goToAddCustomExerciseSets() {
        if (workoutUiState != WorkoutUiState.COMPLETE && workoutUiState != WorkoutUiState.ADD_CUSTOM_EXERCISE_REST) return
        if (!consumeUiAction()) return
        pendingCustomSets = 3
        pendingCustomRest = 60
        workoutUiState = WorkoutUiState.ADD_CUSTOM_EXERCISE_SETS
        persistState()
    }

    fun goToAddCustomExerciseRest() {
        if (workoutUiState != WorkoutUiState.ADD_CUSTOM_EXERCISE_SETS) return
        if (!consumeUiAction()) return
        workoutUiState = WorkoutUiState.ADD_CUSTOM_EXERCISE_REST
        persistState()
    }

    fun cancelAddCustomExercise() {
        if (!consumeUiAction()) return
        workoutUiState = WorkoutUiState.COMPLETE
        persistState()
    }

    fun updatePendingCustomSets(value: Int) {
        pendingCustomSets = value.coerceIn(1, 15)
    }

    fun updatePendingCustomRest(value: Int) {
        pendingCustomRest = value.coerceIn(0, 300)
    }

    fun confirmCustomExercise() {
        if (workoutUiState != WorkoutUiState.ADD_CUSTOM_EXERCISE_REST) return
        if (!consumeUiAction()) return
        
        val w = workout ?: return
        
        val newSets = List(pendingCustomSets) { _ ->
            ActiveSet(
                targetMinReps = 10,
                targetMaxReps = 12,
                targetWeight = 0f,
                plannedRestSeconds = pendingCustomRest
            )
        }
        
        val newExercise = ActiveExercise(
            name = "Custom Exercise $customExerciseCount",
            sets = newSets,
            currentSetIndex = 0
        )
        
        val updatedExercises = w.exercises.toMutableList()
        updatedExercises.add(newExercise)
        
        workout = w.copy(
            exercises = updatedExercises,
            currentExerciseIndex = updatedExercises.size - 1
        )
        customExerciseCount++
        
        workoutUiState = WorkoutUiState.EXERCISE
        persistState()
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
                            CompletedSet(
                                setIndex = setIndex,
                                reps = 0,
                                weight = 0f,
                                actualRestSeconds = 0,
                                skippedRest = true,
                                completedAtEpochMs = System.currentTimeMillis()
                            )
                        }
                    }

                CompletedExercise(name = ex.name, sets = completedSets)
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
        if (!consumeUiAction()) return
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

    fun skipCurrentExercise() {
        if (!consumeUiAction()) return
        val w = workout ?: return
        
        restJob?.cancel()
        isRestRunning = false
        restEndElapsedMs = 0L
        restRemainingSeconds = 0
        appContext?.let { RestAlarmScheduler.cancel(it) }

        val exIndex = w.currentExerciseIndex
        val isLastExercise = exIndex + 1 >= w.exercises.size

        if (isLastExercise) {
            workoutUiState = WorkoutUiState.COMPLETE
            workout = w.copy(
                completedAtEpochMs = System.currentTimeMillis(),
                pendingSync = true
            )
        } else {
            workout = w.copy(
                currentExerciseIndex = exIndex + 1
            )
            workoutUiState = WorkoutUiState.EXERCISE
        }
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

    fun upcomingSetPreview(): UpcomingSetPreview? {
        val w = workout ?: return null
        val exerciseIndex = w.currentExerciseIndex
        val setIndex = currentExercise().currentSetIndex

        var nextExerciseIndex = exerciseIndex
        var nextSetIndex = setIndex + 1

        if (nextSetIndex >= currentExercise().sets.size) {
            nextExerciseIndex += 1
            nextSetIndex = 0
        }

        if (nextExerciseIndex >= w.exercises.size) {
            return null
        }

        val nextExercise = w.exercises[nextExerciseIndex]
        val nextSet = nextExercise.sets[nextSetIndex]
        return UpcomingSetPreview(
            exerciseName = nextExercise.name,
            setNumber = nextSetIndex + 1,
            totalSets = nextExercise.sets.size,
            minReps = nextSet.targetMinReps,
            maxReps = nextSet.targetMaxReps,
            weight = nextSet.targetWeight
        )
    }

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
        val set = currentSet()
        if (set.completedReps == null || set.completedWeight == null) {
            // Defensive guard: never allow rest for an unconfirmed set.
            workoutUiState = WorkoutUiState.EXERCISE
            persistState()
            return
        }

        if (!isRestRunning || restEndElapsedMs <= 0L) {
            val planned = set.plannedRestSeconds
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

        val nextWorkout =
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
        workout = nextWorkout
        if (workoutUiState != WorkoutUiState.COMPLETE) {
            workoutUiState = WorkoutUiState.EXERCISE
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
        workoutUiState = if (workout!!.exercises.isEmpty()) {
            WorkoutUiState.COMPLETE
        } else {
            WorkoutUiState.EXERCISE
        }
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

    private var lastRotaryTimeMs: Long = 0L
    private var fastRotaryCount: Int = 0

    fun handleRotaryDelta(delta: Float): Boolean {
        val now = SystemClock.elapsedRealtime()
        val timeSinceLast = now - lastRotaryTimeMs
        lastRotaryTimeMs = now

        if (timeSinceLast < 100) {
            fastRotaryCount++
        } else {
            fastRotaryCount = 0
        }

        val isAccelerated = fastRotaryCount > 2
        val stepPx = 1f

        when (workoutUiState) {
            WorkoutUiState.CONFIRM_REPS -> {
                rotaryAccum += delta
                val steps = (rotaryAccum / stepPx).toInt()
                if (steps != 0) {
                    rotaryAccum -= steps * stepPx
                    val increment = if (isAccelerated) 3 else 1
                    updatePendingReps(pendingReps + (steps * increment))
                }
                return true
            }

            WorkoutUiState.CONFIRM_WEIGHT -> {
                rotaryAccum += delta
                val steps = (rotaryAccum / stepPx).toInt()
                if (steps != 0) {
                    rotaryAccum -= steps * stepPx
                    val increment = if (isAccelerated) 2.5f else 0.5f
                    updatePendingWeight(pendingWeight + (steps * increment))
                }
                return true
            }

            WorkoutUiState.ADD_CUSTOM_EXERCISE_SETS -> {
                rotaryAccum += delta
                val steps = (rotaryAccum / stepPx).toInt()
                if (steps != 0) {
                    rotaryAccum -= steps * stepPx
                    val increment = 1 // No acceleration for sets
                    updatePendingCustomSets(pendingCustomSets + (steps * increment))
                }
                return true
            }

            WorkoutUiState.ADD_CUSTOM_EXERCISE_REST -> {
                rotaryAccum += delta
                val steps = (rotaryAccum / stepPx).toInt()
                if (steps != 0) {
                    rotaryAccum -= steps * stepPx
                    val increment = if (isAccelerated) 30 else 10
                    updatePendingCustomRest(pendingCustomRest + (steps * increment))
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

    fun onAppHidden() {
        restJob?.cancel()
    }

    private fun consumeUiAction(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastUiActionElapsedMs < uiActionDebounceMs) {
            return false
        }
        lastUiActionElapsedMs = now
        return true
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
            isRestRunning = false
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
        if (!AppVisibilityStore.isVisible()) return
        val token = currentRestToken()
        if (RestHapticStore.wasFired(ctx, token)) return

        RestHapticStore.markFired(ctx, token)
        HapticUtil.vibrate(ctx, "VM")
        android.util.Log.d("RestAlarm", "VM vibrate fired (screen on path)")
    }

    private fun startRestJob() {
        if (!isRestRunning || restEndElapsedMs <= 0L) return
        if (!AppVisibilityStore.isVisible()) return
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
        if (workoutUiState == WorkoutUiState.REST && restEndElapsedMs <= 0L) {
            workoutUiState = WorkoutUiState.EXERCISE
        }
        if (workoutUiState == WorkoutUiState.EXERCISE && state.workout.exercises.isEmpty()) {
            workoutUiState = WorkoutUiState.COMPLETE
        }
        syncRestFromClock()
        startRestJob()
    }
}
