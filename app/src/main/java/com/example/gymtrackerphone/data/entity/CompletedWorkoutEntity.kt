package com.example.gymtrackerphone.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_workouts")
data class CompletedWorkoutEntity(
    @PrimaryKey val workoutId: Int,
    val name: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long
)