package com.example.gymtrackerwatch.domain.model

data class ExerciseHistory(
    val completedAtEpochMs: Long,
    val sets: List<SetHistory>
)

data class SetHistory(
    val reps: Int,
    val weight: Float
)
