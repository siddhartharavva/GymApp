package com.example.gymtrackerwatch.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

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

    fun schedule(context: Context, endElapsedMs: Long, token: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = endElapsedMs.coerceAtLeast(SystemClock.elapsedRealtime())
        if (alarmManager == null) return

        val pi = pendingIntent(context, token)
        val canExact =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pi
                )
            } else {
                // fallback: inexact alarm (no exact-alarm permission required)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    pi
                )
            }
        }.getOrElse {
            // final fallback: basic alarm
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAt,
                pi
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(pendingIntent(context, ""))
    }
}
