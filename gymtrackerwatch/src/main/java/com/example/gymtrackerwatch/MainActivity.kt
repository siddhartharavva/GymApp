package com.example.gymtrackerwatch

import android.os.Bundle
import android.view.WindowManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.gymtrackerwatch.presentation.navigation.WatchNavGraph
import com.example.gymtrackerwatch.presentation.theme.GymTrackerWatchTheme
import com.example.gymtrackerwatch.viewmodel.ActiveWorkoutViewModel
import com.example.gymtrackerwatch.util.AppVisibilityStore

class MainActivity : ComponentActivity() {
    private val vm: ActiveWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.attachContext(applicationContext)

        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

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

    override fun onResume() {
        super.onResume()
        AppVisibilityStore.setVisible(applicationContext, true)
        vm.onAppVisible()
    }

    override fun onPause() {
        super.onPause()
        AppVisibilityStore.setVisible(applicationContext, false)
        vm.onAppHidden()
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        val isRotary =
            ev.action == MotionEvent.ACTION_SCROLL &&
                ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        if (isRotary) {
            val delta = -ev.getAxisValue(MotionEvent.AXIS_SCROLL)
            if (vm.handleRotaryDelta(delta)) {
                return true
            }
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}
