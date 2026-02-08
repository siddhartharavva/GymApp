package com.example.gymtrackerphone.sync.mapper

import com.example.gymtrackerphone.data.entity.ExerciseEntity
import com.example.gymtrackerphone.data.entity.WorkoutEntity
import com.example.gymtrackerphone.data.entity.WorkoutSetEntity
import com.example.gymtrackerphone.sync.dto.ExerciseTemplateDto
import com.example.gymtrackerphone.sync.dto.SetTemplateDto
import com.example.gymtrackerphone.sync.dto.WorkoutTemplateDto

fun mapToTemplate(
    workout: WorkoutEntity,
    exercises: List<ExerciseEntity>,
    sets: List<WorkoutSetEntity>
): WorkoutTemplateDto {

    val exerciseMap = exercises.map { ex ->
        val exSets = sets
            .filter { it.exerciseId == ex.id }
            .sortedBy { it.orderIndex }

        ExerciseTemplateDto(
            name = ex.name,
            sets = exSets.map { set ->
                SetTemplateDto(
                    minReps = set.minReps,
                    maxReps = set.maxReps ?: set.minReps,
                    weight = set.weight,
                    restSeconds = set.restSeconds
                )
            },
            history = emptyList()
        )
    }

    return WorkoutTemplateDto(
        workoutId = workout.id,
        name = workout.name,
        exercises = exerciseMap
    )
}
