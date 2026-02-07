package com.example.gymtrackerwatch

import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import android.util.Log
import com.example.gymtrackerwatch.presentation.navigation.WatchNavGraph
import com.example.gymtrackerwatch.presentation.theme.GymTrackerWatchTheme
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

class MainActivity : ComponentActivity() {
    private val vm: ActiveWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GymTrackerWatchTheme {
                WatchNavGraph(vm = vm)
            }
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val isRotary =
            ev.action == MotionEvent.ACTION_SCROLL &&
                ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        if (isRotary) {
            val delta = -ev.getAxisValue(MotionEvent.AXIS_SCROLL)
            Log.d("RotaryMain", "delta=$delta state=${vm.workoutUiState}")
            if (vm.handleRotaryDelta(delta)) {
                return true
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}
