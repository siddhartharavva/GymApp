package com.example.gymtrackerphone.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtrackerphone.data.entity.CompletedExerciseEntity
import com.example.gymtrackerphone.data.entity.CompletedWorkoutEntity

data class CompletedWorkoutWithExercises(
    @Embedded val workout: CompletedWorkoutEntity,
    @Relation(
        parentColumn = "workoutId",
        entityColumn = "workoutId",
        entity = CompletedExerciseEntity::class
    )
    val exercises: List<CompletedExerciseWithSets>
)
