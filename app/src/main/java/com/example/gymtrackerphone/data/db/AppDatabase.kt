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
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
