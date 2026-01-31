package com.example.gymtrackerphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.gymtrackerphone.ui.navigation.HomeTab
import com.example.gymtrackerphone.data.model.WorkoutUi
import com.example.gymtrackerphone.viewmodel.WorkoutViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onWorkoutClick: (WorkoutUi) -> Unit
) {
    var selectedTab by remember { mutableStateOf<HomeTab>(HomeTab.MyWorkouts) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box {

            // ---- MAIN CONTENT (ABOVE NAVBAR) ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp)   // AppBar height
                    .padding(bottom = 80.dp) // Navbar height
            ) {
                TopAppBar(title = { Text("Gym Tracker") })

                when (selectedTab) {
                    HomeTab.MyWorkouts ->
                        MyWorkoutsScreen(
                            viewModel = viewModel,
                            onWorkoutClick = onWorkoutClick
                        )

                    HomeTab.PastWorkouts ->
                        PastWorkoutsScreen(viewModel)
                }
            }

            // ---- FIXED NAVBAR ----
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(80.dp)
            ) {
                listOf(HomeTab.MyWorkouts, HomeTab.PastWorkouts).forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    }
}