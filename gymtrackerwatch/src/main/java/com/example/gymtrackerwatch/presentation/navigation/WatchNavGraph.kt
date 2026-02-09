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
import androidx.compose.ui.platform.LocalContext


@Composable
fun WatchNavGraph(vm: ActiveWorkoutViewModel) {
    val navController = rememberNavController()
    val hasIncoming by IncomingWorkoutStore.hasWorkout.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        IncomingWorkoutStore.init(context)
        vm.tryResendPending(context)
    }

    // idle → incoming
    LaunchedEffect(hasIncoming, vm.workout, vm.isStarted) {
        if (hasIncoming || (vm.workout != null && !vm.isStarted)) {
            if (navController.currentDestination?.route != "incoming") {
                navController.navigate("incoming") {
                    popUpTo("idle") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // active workout → workout screen
    LaunchedEffect(vm.workout, vm.isStarted) {
        if (vm.workout != null && vm.isStarted) {
            if (navController.currentDestination?.route != "workout") {
                navController.navigate("workout") {
                    popUpTo(navController.graph.id) { inclusive = true }
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
                vm.markStarted()
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
