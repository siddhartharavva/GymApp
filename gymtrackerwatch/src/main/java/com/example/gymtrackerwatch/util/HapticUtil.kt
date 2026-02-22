package com.example.gymtrackerwatch.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object HapticUtil {
    fun vibrate(context: Context, source: String) {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }

        val hasVibrator = vibrator?.hasVibrator() == true
        Log.d("RestAlarm", "$source vibrate request (hasVibrator=$hasVibrator)")
        if (!hasVibrator) return

        val timings = longArrayOf(0, 250, 150, 250, 150, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            vibrator?.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, -1)
        }
    }
}
