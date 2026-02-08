package com.example.gymtrackerwatch.domain.model
import kotlinx.serialization.Serializable

@Serializable
data class CompletedWorkout(
    val workoutId: Int,
    val name: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val exercises: List<CompletedExercise>
)
@Serializable

data class CompletedExercise(
    val name: String,
    val sets: List<CompletedSet>
)
@Serializable

data class CompletedSet(
    val setIndex: Int,
    val reps: Int,
    val weight: Float,
    val actualRestSeconds: Int,
    val skippedRest: Boolean,
    val completedAtEpochMs: Long
)
