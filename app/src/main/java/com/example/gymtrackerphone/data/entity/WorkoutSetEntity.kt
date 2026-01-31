package com.example.gymtrackerphone.data.entity

import androidx.room.*

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val minReps: Int,
    val maxReps: Int?, // null = AMRAP
    val weight: Float,
    val restSeconds: Int
)