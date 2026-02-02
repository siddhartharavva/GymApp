package com.example.gymtrackerphone.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completed_sets",
    foreignKeys = [
        ForeignKey(
            entity = CompletedExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class CompletedSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val reps: Int,
    val weight: Float,
    val actualRestSeconds: Int,
    val skippedRest: Boolean,
    val completedAtEpochMs: Long,
    val orderIndex: Int
)