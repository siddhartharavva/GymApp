package com.example.gymtrackerphone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.example.gymtrackerphone.data.db.AppDatabase

class GymTrackerApp : Application() {

    companion object {
        const val CHANNEL_WORKOUTS = "workout_updates"
    }

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "gymtracker.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_WORKOUTS,
                "Workout updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when workouts are received"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
