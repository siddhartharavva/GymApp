package com.example.gymtrackerphone.mapper

import com.example.gymtrackerphone.data.entity.*
import com.example.gymtrackerphone.data.relation.*
import com.example.gymtrackerphone.sync.dto.CompletedWorkoutDto

fun CompletedWorkoutDto.toEntities(): CompletedWorkoutWithExercises {

    val workoutEntity = CompletedWorkoutEntity(
        name = name,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs
    )

    val exerciseRelations = exercises.mapIndexed { exIndex, exDto ->

        val exerciseEntity = CompletedExerciseEntity(
            workoutId = workoutId,
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
                orderIndex = setIndex
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