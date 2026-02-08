package com.example.gymtrackerphone.data.repository

import androidx.room.withTransaction
import com.example.gymtrackerphone.data.db.AppDatabase
import com.example.gymtrackerphone.data.entity.CompletedExerciseEntity
import com.example.gymtrackerphone.data.entity.CompletedSetEntity
import com.example.gymtrackerphone.data.entity.CompletedWorkoutEntity
import com.example.gymtrackerphone.data.entity.ExerciseEntity
import com.example.gymtrackerphone.data.entity.WorkoutEntity
import com.example.gymtrackerphone.data.entity.WorkoutSetEntity
import com.example.gymtrackerphone.data.mapper.toUi
import com.example.gymtrackerphone.data.model.CompletedWorkoutUi
import com.example.gymtrackerphone.data.model.TemplateImportRow
import com.example.gymtrackerphone.data.model.WorkoutUi
import com.example.gymtrackerphone.data.relation.CompletedWorkoutWithExercises
import com.example.gymtrackerphone.sync.dto.CompletedWorkoutDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val db: AppDatabase
) {
    private val dao = db.workoutDao()

    val workouts: Flow<List<WorkoutUi>> =
        dao.getWorkoutsWithExercises()
            .map { list ->
                list.map { it.toUi() }
            }

    val pastWorkouts: Flow<List<CompletedWorkoutUi>> =
        dao.getCompletedWorkouts()
            .map { list ->
                list.map { it.toUi() }
            }


    // ---------- WORKOUT ----------
    suspend fun addWorkout(name: String) {
        dao.insertWorkout(
            WorkoutEntity(name = name)
        )
    }

    // ---------- READ FOR WATCH SYNC ----------

    suspend fun getWorkoutById(workoutId: Int): WorkoutEntity =
        dao.getWorkoutById(workoutId)

    suspend fun getExercisesForWorkout(workoutId: Int): List<ExerciseEntity> =
        dao.getExercisesForWorkout(workoutId)

    suspend fun getSetsForExercise(exerciseId: Int): List<WorkoutSetEntity> =
        dao.getSetsForExercise(exerciseId)

    suspend fun getRecentCompletedWorkouts(
        templateWorkoutId: Int,
        limit: Int
    ): List<CompletedWorkoutWithExercises> =
        dao.getRecentCompletedWorkouts(templateWorkoutId, limit)

    suspend fun updateWorkout(workoutId: Int, name: String) {
        dao.updateWorkout(workoutId, name)
    }

    suspend fun deleteWorkout(workoutId: Int) {
        dao.deleteWorkoutById(workoutId)
    }

    // ---------- COMPLETED WORKOUT ----------
    suspend fun addCompletedWorkout(dto: CompletedWorkoutDto) {
        db.withTransaction {
            val completedWorkoutId = dao.insertCompletedWorkout(
                CompletedWorkoutEntity(
                    templateWorkoutId = dto.workoutId,
                    name = dto.name,
                    startedAtEpochMs = dto.startedAtEpochMs,
                    completedAtEpochMs = dto.completedAtEpochMs
                )
            ).toInt()

            dto.exercises.forEachIndexed { exIndex, exDto ->
                val completedExerciseId = dao.insertCompletedExercise(
                    CompletedExerciseEntity(
                        workoutId = completedWorkoutId,
                        name = exDto.name,
                        orderIndex = exIndex
                    )
                ).toInt()

                val setEntities = exDto.sets.mapIndexed { setIndex, setDto ->
                    CompletedSetEntity(
                        exerciseId = completedExerciseId,
                        reps = setDto.reps,
                        weight = setDto.weight,
                        actualRestSeconds = setDto.actualRestSeconds,
                        skippedRest = setDto.skippedRest,
                        completedAtEpochMs = setDto.completedAtEpochMs,
                        orderIndex = setIndex
                    )
                }
                if (setEntities.isNotEmpty()) {
                    dao.insertCompletedSets(setEntities)
                }
            }

            // Update template weights from completed workout
            updateTemplateFromCompleted(dto)
        }
    }

    // ---------- EXERCISE ----------
    suspend fun addExercise(workoutId: Int, name: String) {
        dao.insertExercise(
            ExerciseEntity(
                workoutId = workoutId,
                name = name
            )
        )
    }

    suspend fun deleteExercise(exerciseId: Int) {
        dao.deleteExerciseById(exerciseId)
    }

    // ---------- SET ----------
    suspend fun addSet(exerciseId: Int) {
        val orderIndex = dao.getNextSetOrderIndex(exerciseId)
        dao.insertSet(
            WorkoutSetEntity(
                exerciseId = exerciseId,
                minReps = 8,
                maxReps = 12,
                weight = 20f,
                restSeconds = 90,
                orderIndex = orderIndex
            )
        )
    }

    suspend fun deleteSet(setId: Int) {
        dao.deleteSetById(setId)
    }

    suspend fun updateRepRange(setId: Int, min: Int, max: Int) {
        dao.updateRepRange(setId, min, max)
    }

    suspend fun updateWeight(setId: Int, weight: Float) {
        dao.updateWeight(setId, weight)
    }

    suspend fun updateRest(setId: Int, rest: Int) {
        dao.updateRest(setId, rest)
    }

    suspend fun importTemplateWorkouts(rows: List<TemplateImportRow>) {
        if (rows.isEmpty()) return

        db.withTransaction {
            val workouts = rows.groupBy { it.workoutName }
            workouts.forEach { (workoutName, workoutRows) ->
                val workoutId =
                    dao.insertWorkoutReturningId(WorkoutEntity(name = workoutName)).toInt()

                val exercises = workoutRows.groupBy { it.exerciseName }
                exercises.forEach { (exerciseName, exerciseRows) ->
                    val exerciseId =
                        dao.insertExerciseReturningId(
                            ExerciseEntity(
                                workoutId = workoutId,
                                name = exerciseName
                            )
                        ).toInt()

                    exerciseRows.sortedBy { it.setIndex }.forEach { row ->
                        dao.insertSet(
                            WorkoutSetEntity(
                                exerciseId = exerciseId,
                                minReps = row.minReps,
                                maxReps = row.maxReps,
                                weight = row.weight,
                                restSeconds = row.restSeconds,
                                orderIndex = row.setIndex
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun updateTemplateFromCompleted(dto: CompletedWorkoutDto) {
        val exercises = dao.getExercisesForWorkout(dto.workoutId)
        dto.exercises.forEach { completedExercise ->
            val match =
                exercises.firstOrNull { it.name == completedExercise.name }
                    ?: return@forEach
            completedExercise.sets.forEach { completedSet ->
                dao.updateWeightForSetOrder(
                    exerciseId = match.id,
                    orderIndex = completedSet.setIndex,
                    weight = completedSet.weight
                )
            }
        }
    }
}
