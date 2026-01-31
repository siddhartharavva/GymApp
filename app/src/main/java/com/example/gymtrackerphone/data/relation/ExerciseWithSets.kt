package com.example.gymtrackerphone.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtrackerphone.data.entity.*

data class ExerciseWithSets(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<WorkoutSetEntity>
)