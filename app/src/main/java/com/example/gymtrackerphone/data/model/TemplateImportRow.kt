package com.example.gymtrackerphone.data.model

data class TemplateImportRow(
    val workoutName: String,
    val exerciseName: String,
    val setIndex: Int,
    val minReps: Int,
    val maxReps: Int,
    val weight: Float,
    val restSeconds: Int
)
