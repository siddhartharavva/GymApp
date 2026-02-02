package com.example.gymtrackerwatch.sync.store

import androidx.compose.runtime.mutableStateOf
import com.example.gymtrackerwatch.sync.dto.WorkoutTemplateDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object IncomingWorkoutStore {
    private val _hasWorkout = MutableStateFlow(false)
    val hasWorkout: StateFlow<Boolean> = _hasWorkout

    private var workout: WorkoutTemplateDto? = null

    fun store(template: WorkoutTemplateDto) {
        workout = template
        _hasWorkout.value = true
    }

    fun consume(): WorkoutTemplateDto? {
        val w = workout
        workout = null
        _hasWorkout.value = false
        return w
    }
}