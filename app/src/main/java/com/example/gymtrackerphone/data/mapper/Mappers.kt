package com.example.gymtrackerphone.data.mapper

import com.example.gymtrackerphone.data.model.*
import com.example.gymtrackerphone.data.relation.ExerciseWithSets
import com.example.gymtrackerphone.data.relation.WorkoutWithExercises

fun WorkoutWithExercises.toUi(): WorkoutUi =
    WorkoutUi(
        id = workout.id
            ?: error("Workout ID is null. Database corruption."),
        name = workout.name,
        exercises = exercises.map { it.toUi() }
    )

fun ExerciseWithSets.toUi(): ExerciseUi =
    ExerciseUi(
        id = exercise.id
            ?: error("Exercise ID is null. Database corruption."),
        name = exercise.name,
        sets = sets.map { set ->
            WorkoutSetUi(
                id = set.id
                    ?: error("WorkoutSet ID is null. Database corruption."),
                minReps = set.minReps,
                maxReps = set.maxReps ?: set.minReps,
                weight = set.weight,
                restSeconds = set.restSeconds
            )
        }
    )