package com.example.gymtrackerwatch.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.gymtrackerwatch.util.AppVisibilityStore
import android.util.Log
import com.example.gymtrackerwatch.util.HapticUtil
import com.example.gymtrackerwatch.MainActivity

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val token =
            intent?.getStringExtra(RestAlarmScheduler.EXTRA_TOKEN).orEmpty()
        if (token.isNotBlank() && RestHapticStore.wasFired(context, token)) {
            return
        }
        if (AppVisibilityStore.isVisible()) {
            // App is in foreground; ViewModel will handle haptic.
            Log.d("RestAlarm", "Receiver fired but app visible; skipping receiver haptic.")
            return
        }
        if (token.isNotBlank()) {
            RestHapticStore.markFired(context, token)
        }

        val powerManager = context.getSystemService(PowerManager::class.java)
        val wakeLock =
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GymTracker:RestVibrate"
            )
        wakeLock?.acquire(2000)

        HapticUtil.vibrate(context, "Receiver")
        Log.d("RestAlarm", "Receiver vibrate fired (screen off path)")

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("rest_alarm", true)
        }
        context.startActivity(activityIntent)

    }
}
