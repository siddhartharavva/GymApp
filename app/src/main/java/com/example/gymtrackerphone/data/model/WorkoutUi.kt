package com.example.gymtrackerphone.data.model

data class WorkoutUi(
    val id: Int,
    val name: String,
    val exercises: List<ExerciseUi>
)
