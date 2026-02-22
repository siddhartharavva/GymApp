package com.example.gymtrackerwatch.util

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.example.gymtrackerwatch.MainActivity
import android.util.Log

object RestAlarmScheduler {
    private const val ACTION_REST_ALARM =
        "com.example.gymtrackerwatch.ACTION_REST_ALARM"
    const val EXTRA_TOKEN = "extra_rest_token"

    private fun pendingIntent(context: Context, token: String): PendingIntent {
        val intent = Intent(context, RestAlarmReceiver::class.java)
            .setAction(ACTION_REST_ALARM)
            .putExtra(EXTRA_TOKEN, token)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, endElapsedMs: Long, token: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (alarmManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(
                    "RestAlarm",
                    "Exact alarm NOT allowed; alarm not scheduled."
                )
                return
            }
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val delayMs = (endElapsedMs - nowElapsed).coerceAtLeast(0L)
        val triggerAtWall = nowWall + delayMs
        val pi = pendingIntent(context, token)
        runCatching {
            alarmManager.setAlarmClock(
                AlarmClockInfo(triggerAtWall, showIntent(context)),
                pi
            )
            Log.d(
                "RestAlarm",
                "Scheduled alarmClock at wall=${triggerAtWall} (delayMs=${delayMs}) token=$token"
            )
        }.onFailure { e ->
            Log.e("RestAlarm", "Failed to schedule alarmClock", e)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(pendingIntent(context, ""))
        Log.d("RestAlarm", "Cancelled alarm")
    }
}
