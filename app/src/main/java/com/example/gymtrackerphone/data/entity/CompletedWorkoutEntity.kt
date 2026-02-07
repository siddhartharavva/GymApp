package com.example.gymtrackerphone.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completed_workouts",
    indices = [Index("templateWorkoutId")]
)
data class CompletedWorkoutEntity(
    @PrimaryKey(autoGenerate = true) val workoutId: Int = 0,
    val templateWorkoutId: Int,
    val name: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long
)
