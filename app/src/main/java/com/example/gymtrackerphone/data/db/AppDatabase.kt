package com.example.gymtrackerphone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gymtrackerphone.data.dao.WorkoutDao
import com.example.gymtrackerphone.data.entity.*

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseEntity::class,
        WorkoutSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}