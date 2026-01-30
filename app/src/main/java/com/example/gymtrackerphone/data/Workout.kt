package com.example.gymtrackerphone.data

data class Workout(
    val id: Int,
    val name: String,
    val exercises: List<Exercise>
)

data class Exercise(
    val id: Int,
    val workoutId: Int,
    val name: String,
    val sets: List<WorkoutSet>
)

data class WorkoutSet(
    val id: Int,
    val reps: Int,
    val weight: Float
)