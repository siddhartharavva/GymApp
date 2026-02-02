package com.example.gymtrackerphone.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.gymtrackerphone.data.entity.CompletedExerciseEntity
import com.example.gymtrackerphone.data.entity.CompletedSetEntity

data class CompletedExerciseWithSets(
    @Embedded
    val exercise: CompletedExerciseEntity,

    @Relation(
        parentColumn = "id",          // CompletedExerciseEntity.id
        entityColumn = "exerciseId",  // CompletedSetEntity.exerciseId
        entity = CompletedSetEntity::class
    )
    val sets: List<CompletedSetEntity>
)