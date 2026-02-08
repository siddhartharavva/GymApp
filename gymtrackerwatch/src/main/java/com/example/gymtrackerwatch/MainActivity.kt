package com.example.gymtrackerwatch

import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import com.example.gymtrackerwatch.presentation.navigation.WatchNavGraph
import com.example.gymtrackerwatch.presentation.theme.GymTrackerWatchTheme
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel

class MainActivity : ComponentActivity() {
    private val vm: ActiveWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

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
