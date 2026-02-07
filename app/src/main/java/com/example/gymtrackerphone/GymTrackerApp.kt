package com.example.gymtrackerphone

import android.app.Application
import androidx.room.Room
import com.example.gymtrackerphone.data.db.AppDatabase

class GymTrackerApp : Application() {

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
    }
}
