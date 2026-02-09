package com.example.gymtrackerwatch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActiveWorkout(
    val workoutId: Int,
    val name: String,
    val exercises: List<ActiveExercise>,
    val currentExerciseIndex: Int = 0,

    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val pendingSync: Boolean = true
)
