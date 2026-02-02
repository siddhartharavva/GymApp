package com.example.gymtrackerwatch.domain.model

data class ActiveExercise(
    val name: String,
    val sets: List<ActiveSet>,
    val currentSetIndex: Int = 0
)