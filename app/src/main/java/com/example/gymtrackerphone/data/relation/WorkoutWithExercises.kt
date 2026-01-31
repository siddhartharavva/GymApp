package com.example.gymtrackerphone.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtrackerphone.data.entity.*

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId",
        entity = ExerciseEntity::class
    )
    val exercises: List<ExerciseWithSets>
)