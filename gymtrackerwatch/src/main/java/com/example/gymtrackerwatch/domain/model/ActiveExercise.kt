package com.example.gymtrackerwatch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActiveExercise(
    val name: String,
    val sets: List<ActiveSet>,
    val currentSetIndex: Int = 0,
    val history: List<ExerciseHistory> = emptyList()
)
