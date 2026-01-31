package com.example.gymtrackerphone.data.model

data class ExerciseUi(
    val id: Int,
    val name: String,
    val sets: List<WorkoutSetUi>
)