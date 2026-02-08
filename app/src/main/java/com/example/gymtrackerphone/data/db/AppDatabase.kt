package com.example.gymtrackerphone.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.gymtrackerphone.data.dao.WorkoutDao
import com.example.gymtrackerphone.data.entity.*

@Database(
    entities = [
        WorkoutEntity::class,
        ExerciseEntity::class,
        WorkoutSetEntity::class,
        CompletedWorkoutEntity::class,
        CompletedExerciseEntity::class,
        CompletedSetEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
