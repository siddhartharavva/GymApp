package com.example.gymtrackerwatch.presentation.util

import com.example.gymtrackerwatch.domain.model.SetHistory

fun formatHistorySets(sets: List<SetHistory>): String =
    sets.joinToString(" • ") { "${it.reps}x${formatWeight(it.weight)}" }

fun formatWeight(weight: Float): String =
    if (weight % 1f == 0f) {
        weight.toInt().toString()
    } else {
        weight.toString()
    }
