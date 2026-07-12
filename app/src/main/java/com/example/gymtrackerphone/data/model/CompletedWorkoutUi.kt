package com.example.gymtrackerphone.data.model

data class CompletedWorkoutUi(
    val id: Int,
    val templateWorkoutId: Int,
    val name: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val exercises: List<CompletedExerciseUi>
)

data class CompletedExerciseUi(
    val id: Int,
    val name: String,
    val sets: List<CompletedSetUi>
)

data class CompletedSetUi(
    val id: Int,
    val reps: Int,
    val weight: Float,
    val actualRestSeconds: Int,
    val skippedRest: Boolean,
    val completedAtEpochMs: Long
)
