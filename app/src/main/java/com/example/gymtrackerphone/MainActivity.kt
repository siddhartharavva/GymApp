package com.example.gymtrackerphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.gymtrackerphone.ui.HomeScreen
import com.example.gymtrackerphone.ui.theme.GymTrackerPhoneTheme
import com.example.gymtrackerphone.ui.WorkoutDetailsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackerPhoneTheme {

                val navController = rememberNavController()
                val workoutViewModel: WorkoutViewModel = viewModel()
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {

                    composable("home") {
                        HomeScreen(
                            viewModel = workoutViewModel,
                            onWorkoutClick = { workout ->
                                navController.navigate("details/${workout.id}")
                            }
                        )
                    }

                    composable(
                        route = "details/{workoutId}"
                    ) { backStackEntry ->
                        val workoutId =
                            backStackEntry.arguments?.getString("workoutId")?.toInt()

                        val workoutViewModel: WorkoutViewModel = viewModel()

                        WorkoutDetailsScreen(
                            workoutId = workoutId!!,
                            viewModel = workoutViewModel
                        )                    }
                }
            }
        }
    }
}

