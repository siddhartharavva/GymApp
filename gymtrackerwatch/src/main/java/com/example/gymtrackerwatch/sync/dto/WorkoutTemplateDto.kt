package com.example.gymtrackerwatch.sync.dto
import kotlinx.serialization.Serializable


@Serializable
data class WorkoutTemplateDto(
    val workoutId: Int,
    val name: String,
    val exercises: List<ExerciseTemplateDto>
)