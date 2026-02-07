package com.example.gymtrackerwatch.sync.dto
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseTemplateDto(
    val name: String,
    val sets: List<SetTemplateDto>,
    val history: List<ExerciseHistoryDto> = emptyList()
)
