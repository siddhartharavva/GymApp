package com.example.gymtrackerwatch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseHistory(
    val completedAtEpochMs: Long,
    val sets: List<SetHistory>
)

@Serializable
data class SetHistory(
    val reps: Int,
    val weight: Float
)
