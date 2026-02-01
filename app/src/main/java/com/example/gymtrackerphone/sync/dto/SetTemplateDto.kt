package com.example.gymtrackerphone.sync.dto
import kotlinx.serialization.Serializable

@Serializable
data class SetTemplateDto(
    val minReps: Int,
    val maxReps: Int,
    val weight: Float,
    val restSeconds: Int
)