package com.example.gymtrackerphone.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerphone.data.repository.WorkoutRepository
import com.example.gymtrackerphone.data.model.TemplateImportRow
import com.example.gymtrackerphone.sync.dto.CompletedExercise
import com.example.gymtrackerphone.sync.dto.CompletedSet
import com.example.gymtrackerphone.sync.dto.CompletedWorkoutDto
import com.example.gymtrackerphone.sync.dto.ExerciseHistoryDto
import com.example.gymtrackerphone.sync.dto.ExerciseTemplateDto
import com.example.gymtrackerphone.sync.dto.SetHistoryDto
import com.example.gymtrackerphone.sync.dto.SetTemplateDto
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto
import com.example.gymtrackerphone.sync.sender.WorkoutSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val pastWorkouts =
        repository.pastWorkouts
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    val workouts = repository.workouts
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun addWorkout(name: String) = launch {
        repository.addWorkout(name)
    }

    fun deleteWorkout(workoutId: Int) = launch {
        repository.deleteWorkout(workoutId)
    }

    /** Builds DTO only – NO sending, NO context */
    suspend fun buildWorkoutTemplate(workoutId: Int): WorkoutTemplateDto {
        val workout = repository.getWorkoutById(workoutId)
        val exercises = repository.getExercisesForWorkout(workoutId)
        val recentCompleted =
            repository.getRecentCompletedWorkouts(
                templateWorkoutId = workoutId,
                limit = 2
            )

        val historyByExerciseIndex =
            mutableMapOf<Int, MutableList<ExerciseHistoryDto>>()

        recentCompleted.forEach { completed ->
            val completedAt = completed.workout.completedAtEpochMs

            completed.exercises
                .sortedBy { it.exercise.orderIndex }
                .forEach { exWithSets ->
                    val index = exWithSets.exercise.orderIndex
                    val sets =
                        exWithSets.sets
                            .sortedBy { it.orderIndex }
                            .map { set ->
                                SetHistoryDto(
                                    reps = set.reps,
                                    weight = set.weight
                                )
                            }
                    if (sets.isNotEmpty()) {
                        val list =
                            historyByExerciseIndex.getOrPut(index) { mutableListOf() }
                        list.add(
                            ExerciseHistoryDto(
                                completedAtEpochMs = completedAt,
                                sets = sets
                            )
                        )
                    }
                }
        }

        return WorkoutTemplateDto(
            workoutId = workout.id,
            name = workout.name,
            exercises = exercises.mapIndexed { index, exercise ->
                val sets =
                    repository.getSetsForExercise(exercise.id)
                        .sortedBy { it.orderIndex }
                ExerciseTemplateDto(
                    name = exercise.name,
                    sets = sets.map {
                        SetTemplateDto(
                            minReps = it.minReps,
                            maxReps = it.maxReps ?: it.minReps,
                            weight = it.weight,
                            restSeconds = it.restSeconds
                        )
                    },
                    history = historyByExerciseIndex[index] ?: emptyList()
                )
            }
        )
    }

    fun sendWorkoutToWatch(
        context: Context,
        workoutId: Int
    ) {
        viewModelScope.launch {
            val dto = buildWorkoutTemplate(workoutId)
            WorkoutSender.sendWorkout(context, dto)
        }
    }

    fun addExercise(workoutId: Int, name: String) = launch {
        repository.addExercise(workoutId, name)
    }

    fun deleteExercise(exerciseId: Int) = launch {
        repository.deleteExercise(exerciseId)
    }

    fun addSet(exerciseId: Int) = launch {
        repository.addSet(exerciseId)
    }

    fun deleteSet(setId: Int) = launch {
        repository.deleteSet(setId)
    }

    fun updateRepRange(setId: Int, min: Int, max: Int) = launch {
        repository.updateRepRange(setId, min, max)
    }

    fun updateWeight(setId: Int, weight: Float) = launch {
        repository.updateWeight(setId, weight)
    }

    fun updateRest(setId: Int, rest: Int) = launch {
        repository.updateRest(setId, rest)
    }

    fun updateWorkout(workoutId: Int, name: String) = launch {
        repository.updateWorkout(workoutId, name)
    }

    fun importCompletedCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = readText(context, uri) ?: return@launch
            val rows = parseCsv(text)
            if (rows.size < 2) return@launch

            val header = rows.first().map { it.trim().lowercase(Locale.getDefault()) }
            val idxWorkout = headerIndex(header, "workout")
            val idxCompletedAt = headerIndex(header, "completed at")
            val idxDuration = headerIndex(header, "duration (min)", "duration")
            val idxExercise = headerIndex(header, "exercise")
            val idxSet = headerIndex(header, "set")
            val idxReps = headerIndex(header, "reps")
            val idxWeight = headerIndex(header, "weight")

            if (listOf(idxWorkout, idxExercise, idxSet, idxReps, idxWeight).any { it == -1 }) {
                return@launch
            }

            val groups = mutableMapOf<String, MutableList<CompletedRow>>()
            rows.drop(1).forEach { row ->
                val workoutName = row.getOrNull(idxWorkout)?.trim().orEmpty()
                val exerciseName = row.getOrNull(idxExercise)?.trim().orEmpty()
                if (workoutName.isBlank() || exerciseName.isBlank()) return@forEach

                val completedAtText = row.getOrNull(idxCompletedAt)?.trim().orEmpty()
                val durationMin = row.getOrNull(idxDuration)?.trim()?.toLongOrNull() ?: 0L
                val setIndex = (row.getOrNull(idxSet)?.trim()?.toIntOrNull() ?: 1) - 1
                val reps = row.getOrNull(idxReps)?.trim()?.toIntOrNull() ?: 0
                val weight = row.getOrNull(idxWeight)?.trim()?.toFloatOrNull() ?: 0f

                val key = "$workoutName|$completedAtText"
                val list = groups.getOrPut(key) { mutableListOf() }
                list.add(
                    CompletedRow(
                        workoutName = workoutName,
                        completedAtText = completedAtText,
                        durationMin = durationMin,
                        exerciseName = exerciseName,
                        setIndex = setIndex,
                        reps = reps,
                        weight = weight
                    )
                )
            }

            groups.values.forEach { rowsForWorkout ->
                val workoutName = rowsForWorkout.first().workoutName
                val completedAtText = rowsForWorkout.first().completedAtText
                val durationMin = rowsForWorkout.first().durationMin

                val completedAtEpoch =
                    parseDateTime(completedAtText) ?: System.currentTimeMillis()
                val startedAtEpoch =
                    if (durationMin > 0) {
                        completedAtEpoch - durationMin * 60_000L
                    } else {
                        completedAtEpoch
                    }

                val exercises =
                    rowsForWorkout.groupBy { it.exerciseName }
                        .map { (exerciseName, setRows) ->
                            CompletedExercise(
                                name = exerciseName,
                                sets = setRows
                                    .sortedBy { it.setIndex }
                                    .map { row ->
                                        CompletedSet(
                                            setIndex = row.setIndex,
                                            reps = row.reps,
                                            weight = row.weight,
                                            actualRestSeconds = 0,
                                            skippedRest = false,
                                            completedAtEpochMs = completedAtEpoch
                                        )
                                    }
                            )
                        }

                val dto =
                    CompletedWorkoutDto(
                        workoutId = -1,
                        name = workoutName,
                        startedAtEpochMs = startedAtEpoch,
                        completedAtEpochMs = completedAtEpoch,
                        exercises = exercises
                    )

                repository.addCompletedWorkout(dto)
            }
        }
    }

    fun importTemplateCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val text = readText(context, uri) ?: return@launch
            val rows = parseCsv(text)
            if (rows.size < 2) return@launch

            val header = rows.first().map { it.trim().lowercase(Locale.getDefault()) }
            val idxWorkout = headerIndex(header, "workout")
            val idxExercise = headerIndex(header, "exercise")
            val idxSet = headerIndex(header, "set")
            val idxMin = headerIndex(header, "minreps", "min reps", "min_reps")
            val idxMax = headerIndex(header, "maxreps", "max reps", "max_reps")
            val idxReps = headerIndex(header, "reps")
            val idxWeight = headerIndex(header, "weight")
            val idxRest = headerIndex(header, "restseconds", "rest seconds", "rest")

            if (listOf(idxWorkout, idxExercise, idxWeight).any { it == -1 }) {
                return@launch
            }

            val rowsParsed = mutableListOf<TemplateImportRow>()
            val counters = mutableMapOf<String, Int>()

            rows.drop(1).forEach { row ->
                val workoutName = row.getOrNull(idxWorkout)?.trim().orEmpty()
                val exerciseName = row.getOrNull(idxExercise)?.trim().orEmpty()
                if (workoutName.isBlank() || exerciseName.isBlank()) return@forEach

                val setIndex =
                    row.getOrNull(idxSet)?.trim()?.toIntOrNull()?.minus(1)
                        ?: run {
                            val key = "$workoutName|$exerciseName"
                            val next = counters.getOrDefault(key, 0)
                            counters[key] = next + 1
                            next
                        }

                val minReps =
                    row.getOrNull(idxMin)?.trim()?.toIntOrNull()
                        ?: row.getOrNull(idxReps)?.trim()?.toIntOrNull()
                        ?: 8
                val maxReps =
                    row.getOrNull(idxMax)?.trim()?.toIntOrNull()
                        ?: row.getOrNull(idxReps)?.trim()?.toIntOrNull()
                        ?: minReps
                val weight =
                    row.getOrNull(idxWeight)?.trim()?.toFloatOrNull() ?: 0f
                val rest =
                    row.getOrNull(idxRest)?.trim()?.toIntOrNull() ?: 90

                rowsParsed.add(
                    TemplateImportRow(
                        workoutName = workoutName,
                        exerciseName = exerciseName,
                        setIndex = setIndex,
                        minReps = minReps,
                        maxReps = maxReps,
                        weight = weight,
                        restSeconds = rest
                    )
                )
            }

            if (rowsParsed.isNotEmpty()) {
                repository.importTemplateWorkouts(rowsParsed)
            }
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private suspend fun readText(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
        }

    private fun parseCsv(text: String): List<List<String>> {
        val lines =
            text.split("\n")
                .map { it.trimEnd('\r') }
                .filter { it.isNotBlank() }

        return lines.map { parseCsvLine(it) }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }

                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun headerIndex(header: List<String>, vararg names: String): Int {
        for (name in names) {
            val idx = header.indexOf(name.lowercase(Locale.getDefault()))
            if (idx != -1) return idx
        }
        return -1
    }

    private fun parseDateTime(text: String): Long? {
        if (text.isBlank()) return null
        return try {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            formatter.parse(text)?.time
        } catch (_: Exception) {
            null
        }
    }

    private data class CompletedRow(
        val workoutName: String,
        val completedAtText: String,
        val durationMin: Long,
        val exerciseName: String,
        val setIndex: Int,
        val reps: Int,
        val weight: Float
    )

}
