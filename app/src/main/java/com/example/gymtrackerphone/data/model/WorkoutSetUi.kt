package com.example.gymtrackerphone.data.model

data class WorkoutSetUi(
    val id: Int,
    val minReps: Int,
    val maxReps: Int,
    val weight: Float,
    val restSeconds: Int
)