package com.example.gymtrackerphone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import com.example.gymtrackerphone.data.model.WorkoutUi
import com.example.gymtrackerphone.data.repository.WorkoutRepository
import com.example.gymtrackerphone.ui.HomeScreen
import com.example.gymtrackerphone.ui.theme.GymTrackerPhoneTheme
import com.example.gymtrackerphone.ui.WorkoutDetailsScreen
import com.example.gymtrackerphone.viewmodel.WorkoutViewModelFactory
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            GymTrackerPhoneTheme {

                val navController = rememberNavController()

                val app = application as GymTrackerApp
                val repository = WorkoutRepository(app.database)

                val workoutViewModel: WorkoutViewModel = viewModel(
                    factory = WorkoutViewModelFactory(repository)
                )

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

                    composable("details/{workoutId}") { backStackEntry ->
                        val workoutId =
                            backStackEntry.arguments?.getString("workoutId")!!.toInt()

                        WorkoutDetailsScreen(
                            workoutId = workoutId,
                            viewModel = workoutViewModel
                        )
                    }
                }
            }
        }
    }
}
