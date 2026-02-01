package com.example.gymtrackerphone.sync.dto
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseTemplateDto(
    val name: String,
    val sets: List<SetTemplateDto>
)