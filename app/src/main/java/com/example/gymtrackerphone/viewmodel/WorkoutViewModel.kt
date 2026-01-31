package com.example.gymtrackerphone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtrackerphone.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

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