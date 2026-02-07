package com.example.gymtrackerphone.sync.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseHistoryDto(
    val completedAtEpochMs: Long,
    val sets: List<SetHistoryDto>
)

@Serializable
data class SetHistoryDto(
    val reps: Int,
    val weight: Float
)
