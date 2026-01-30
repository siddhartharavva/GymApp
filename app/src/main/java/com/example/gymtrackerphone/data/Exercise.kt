package com.example.gymtrackerphone.data


data class Exercise(
    val id: Int,
    val workoutId: Int,
    val name: String,
    val sets: List<WorkoutSet>
)