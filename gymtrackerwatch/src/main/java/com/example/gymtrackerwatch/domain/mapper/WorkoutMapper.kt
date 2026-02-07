package com.example.gymtrackerwatch.domain.mapper

import com.example.gymtrackerwatch.domain.model.*
import com.example.gymtrackerwatch.sync.dto.WorkoutTemplateDto

fun WorkoutTemplateDto.toActiveWorkout(): ActiveWorkout {
    return ActiveWorkout(
        workoutId = workoutId,
        name = name,
        startedAtEpochMs = System.currentTimeMillis(),
        exercises = exercises.map { ex ->
            ActiveExercise(
                name = ex.name,
                sets = ex.sets.map {
                    ActiveSet(
                        targetMinReps = it.minReps,
                        targetMaxReps = it.maxReps,
                        targetWeight = it.weight,
                        plannedRestSeconds = it.restSeconds
                    )
                },
                history = ex.history.map { history ->
                    ExerciseHistory(
                        completedAtEpochMs = history.completedAtEpochMs,
                        sets = history.sets.map { set ->
                            SetHistory(
                                reps = set.reps,
                                weight = set.weight
                            )
                        }
                    )
                }
            )
        }
    )
}
