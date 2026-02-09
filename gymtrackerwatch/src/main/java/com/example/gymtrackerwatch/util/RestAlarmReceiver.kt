package com.example.gymtrackerwatch.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.gymtrackerwatch.notifications.WatchNotificationHelper

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val token =
            intent?.getStringExtra(RestAlarmScheduler.EXTRA_TOKEN).orEmpty()
        if (token.isNotBlank() && RestHapticStore.wasFired(context, token)) {
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

        val vibrator = context.getSystemService(Vibrator::class.java)
        val effect =
            VibrationEffect.createOneShot(
                800,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        vibrator?.vibrate(effect, attrs)

        WatchNotificationHelper.showRestComplete(context)
    }
}
