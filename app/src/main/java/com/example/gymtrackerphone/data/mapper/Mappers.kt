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
        sets = sets.sortedBy { it.orderIndex }.map {
            WorkoutSetUi(
                id = it.id,
                minReps = it.minReps,
                maxReps = it.maxReps ?: it.minReps,
                weight = it.weight,
                restSeconds = it.restSeconds
            )
        }
    )

fun CompletedWorkoutWithExercises.toUi(): CompletedWorkoutUi =
    CompletedWorkoutUi(
        id = workout.workoutId,
        templateWorkoutId = workout.templateWorkoutId,
        name = workout.name,
        startedAtEpochMs = workout.startedAtEpochMs,
        completedAtEpochMs = workout.completedAtEpochMs,
        exercises = exercises
            .sortedBy { it.exercise.orderIndex }
            .map { it.toUi() }
    )

fun CompletedExerciseWithSets.toUi(): CompletedExerciseUi =
    CompletedExerciseUi(
        name = exercise.name,
        sets = sets
            .sortedBy { it.orderIndex }
            .map {
                CompletedSetUi(
                    reps = it.reps,
                    weight = it.weight,
                    actualRestSeconds = it.actualRestSeconds,
                    skippedRest = it.skippedRest,
                    completedAtEpochMs = it.completedAtEpochMs
                )
            }
    )
