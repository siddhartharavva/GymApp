package com.example.gymtrackerphone.data

data class WorkoutSet(
    val id: Int,
    val minReps: Int = 8,
    val maxReps: Int = 12,
    val weight: Float = 0f,
    val restSeconds: Int = 60
)