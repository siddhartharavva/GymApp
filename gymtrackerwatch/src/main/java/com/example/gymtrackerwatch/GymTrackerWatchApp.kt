package com.example.gymtrackerwatch

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.gymtrackerwatch.util.AppVisibility

class GymTrackerWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    AppVisibility.isVisible = true
                }

                override fun onStop(owner: LifecycleOwner) {
                    AppVisibility.isVisible = false
                }
            }
        )
    }
}
