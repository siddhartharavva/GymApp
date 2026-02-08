package com.example.gymtrackerphone.sync.mapper

import com.example.gymtrackerphone.data.entity.*
import com.example.gymtrackerphone.data.relation.*
import com.example.gymtrackerphone.sync.dto.CompletedWorkoutDto

fun CompletedWorkoutDto.toEntities(): CompletedWorkoutWithExercises {

    val workoutEntity = CompletedWorkoutEntity(
        templateWorkoutId = workoutId,
        name = name,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs
    )

    val exerciseRelations = exercises.mapIndexed { exIndex, exDto ->

        val exerciseEntity = CompletedExerciseEntity(
            workoutId = 0, // filled after insert
            name = exDto.name,
            orderIndex = exIndex
        )

        val setEntities = exDto.sets.mapIndexed { setIndex, setDto ->
            CompletedSetEntity(
                exerciseId = 0, // filled after insert
                reps = setDto.reps,
                weight = setDto.weight,
                actualRestSeconds = setDto.actualRestSeconds,
                skippedRest = setDto.skippedRest,
                completedAtEpochMs = setDto.completedAtEpochMs,
                orderIndex = setDto.setIndex
            )
        }

        CompletedExerciseWithSets(
            exercise = exerciseEntity,
            sets = setEntities
        )
    }

    return CompletedWorkoutWithExercises(
        workout = workoutEntity,
        exercises = exerciseRelations
    )
}
