package com.example.gymtrackerwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrackerwatch.presentation.navigation.WatchNavGraph
import com.example.gymtrackerwatch.presentation.theme.GymTrackerWatchTheme
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GymTrackerWatchTheme {
                val vm: ActiveWorkoutViewModel = viewModel()
                WatchNavGraph(vm = vm)
            }
        }
    }
}