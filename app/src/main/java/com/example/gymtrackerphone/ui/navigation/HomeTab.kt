package com.example.gymtrackerphone.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.graphics.vector.ImageVector

sealed class HomeTab(
    val label: String,
    val icon: ImageVector
) {
    object MyWorkouts : HomeTab(
        label = "My Workouts",
        icon = Icons.Outlined.FitnessCenter
    )

    object PastWorkouts : HomeTab(
        label = "Past Workouts",
        icon = Icons.Outlined.History
    )
}