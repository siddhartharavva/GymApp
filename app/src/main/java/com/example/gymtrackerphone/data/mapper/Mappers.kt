package com.example.gymtrackerphone.data.mapper

import com.example.gymtrackerphone.data.model.*
import com.example.gymtrackerphone.data.relation.*



fun WorkoutWithExercises.toUi(): WorkoutUi =
    WorkoutUi(
        id = workout.id,
        name = workout.name,
        exercises = exercises.map { it.toUi() }
    )

fun ExerciseWithSets.toUi(): ExerciseUi =
    ExerciseUi(
        id = exercise.id,
        name = exercise.name,
        sets = sets.map {
            WorkoutSetUi(
                id = it.id,
                minReps = it.minReps,
                maxReps = it.maxReps ?: it.minReps,
                weight = it.weight,
                restSeconds = it.restSeconds
            )
        }
    )