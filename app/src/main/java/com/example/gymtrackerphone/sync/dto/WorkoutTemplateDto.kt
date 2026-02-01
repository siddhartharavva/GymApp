package com.example.gymtrackerphone.sync.dto
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplateDto(
    val workoutId: Int,
    val name: String,
    val exercises: List<ExerciseTemplateDto>
)

