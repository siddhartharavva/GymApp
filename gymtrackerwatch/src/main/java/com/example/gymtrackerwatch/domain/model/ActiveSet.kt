package com.example.gymtrackerwatch.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActiveSet(
    val targetMinReps: Int,
    val targetMaxReps: Int,
    val targetWeight: Float,
    val plannedRestSeconds: Int,

    val completedReps: Int? = null,
    val completedWeight: Float? = null,
    val actualRestSeconds: Int? = null,
    val skippedRest: Boolean = false,
    val completedAtEpochMs: Long? = null
)
