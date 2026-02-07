package com.example.gymtrackerwatch.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymtrackerwatch.presentation.screen.*
import com.example.gymtrackerwatch.sync.store.IncomingWorkoutStore
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel
import androidx.compose.runtime.getValue


@Composable
fun WatchNavGraph(vm: ActiveWorkoutViewModel) {
    val navController = rememberNavController()
    val hasWorkout by vm.hasWorkout.collectAsState()

    // idle → incoming
    LaunchedEffect(hasWorkout) {
        if (hasWorkout) {
            if (navController.currentDestination?.route != "incoming") {
                navController.navigate("incoming") {
                    popUpTo("idle") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // workout → complete (global, once)
    LaunchedEffect(vm.isWorkoutCompleted) {
        if (vm.isWorkoutCompleted) {
            if (navController.currentDestination?.route != "complete") {
                navController.navigate("complete") {
                    popUpTo("workout") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // when workout resets to null, go back to idle
    LaunchedEffect(vm.workout) {
        if (vm.workout == null) {
            if (navController.currentDestination?.route != "idle") {
                navController.navigate("idle") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "idle"
    ) {
        composable("idle") {
            IdleScreen()
        }

        composable("incoming") {
            IncomingWorkoutScreen(vm) {
                navController.navigate("workout") {
                    popUpTo("incoming") { inclusive = true }
                }
            }
        }

        composable("workout") {
            WorkoutFlowScreen(vm) // ENUM controls inner flow
        }

        composable("complete") {
            WorkoutCompleteScreen(vm) // no lambda
        }
    }
}
