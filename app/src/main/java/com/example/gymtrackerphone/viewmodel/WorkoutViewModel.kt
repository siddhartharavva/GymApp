package com.example.gymtrackerphone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerphone.data.repository.WorkoutRepository
import com.example.gymtrackerphone.sync.dto.ExerciseTemplateDto
import com.example.gymtrackerphone.sync.dto.SetTemplateDto
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto
import com.example.gymtrackerphone.sync.sender.WorkoutSender
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val pastWorkouts =
        WorkoutRepository.pastWorkouts
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

        return WorkoutTemplateDto(
            workoutId = workout.id,
            name = workout.name,
            exercises = exercises.map { exercise ->
                val sets = repository.getSetsForExercise(exercise.id)
                ExerciseTemplateDto(
                    name = exercise.name,
                    sets = sets.map {
                        SetTemplateDto(
                            minReps = it.minReps,
                            maxReps = it.maxReps ?: it.minReps,
                            weight = it.weight,
                            restSeconds = it.restSeconds
                        )
                    }
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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}