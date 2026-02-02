package com.example.gymtrackerphone.sync.dto

import kotlinx.serialization.Serializable

@Serializable
data class CompletedWorkoutDto(
    val workoutId: Int,
    val name: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val exercises: List<CompletedExercise>
)
@Serializable

data class CompletedExercise(
    val name: String,
    val sets: List<CompletedSet>
)
@Serializable

data class CompletedSet(
    val reps: Int,
    val weight: Float,
    val actualRestSeconds: Int,
    val skippedRest: Boolean,
    val completedAtEpochMs: Long
)