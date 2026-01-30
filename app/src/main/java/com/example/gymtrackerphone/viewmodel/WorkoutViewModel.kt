package com.example.gymtrackerphone.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.gymtrackerphone.data.Workout
import com.example.gymtrackerphone.data.Exercise
import com.example.gymtrackerphone.data.WorkoutSet



class WorkoutViewModel : ViewModel() {

    // ---- STATE ----
    private val _workouts = mutableStateListOf(
        Workout(
            id = 1,
            name = "Push Day",
            exercises = listOf(
                Exercise(
                    id = 1,
                    workoutId = 1,
                    name = "Bench Press",
                    sets = listOf(
                        WorkoutSet(
                            id = 1,
                            minReps = 8,
                            maxReps = 12,
                            weight = 60f,
                            restSeconds = 90
                        ),
                        WorkoutSet(
                            id = 2,
                            minReps = 8,
                            maxReps = 12,
                            weight = 60f,
                            restSeconds = 90
                        )
                    )
                )
            )
        ),
        Workout(
            id = 2,
            name = "Pull Day",
            exercises = emptyList()
        )
    )

    val workouts: List<Workout> = _workouts


    fun getWorkoutById(id: Int): Workout? {
        return _workouts.find { it.id == id }
    }


    fun addWorkout(name: String) {
        _workouts.add(
            Workout(
                id = _workouts.size + 1,
                name = name,
                exercises = emptyList()
            )
        )
    }

    fun updateWorkout(id: Int, name: String) {
        val index = _workouts.indexOfFirst { it.id == id }
        if (index == -1) return

        _workouts[index] = _workouts[index].copy(name = name)
    }

    fun deleteWorkout(id: Int) {
        _workouts.removeAll { it.id == id }
    }

    // ---- EXERCISE CRUD ----
    fun addExercise(workoutId: Int, name: String) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]

        val newExercise = Exercise(
            id = workout.exercises.size + 1,
            workoutId = workoutId,
            name = name,
            sets = listOf(
                WorkoutSet(
                    id = 1,
                    minReps = 8,
                    maxReps = 12,
                    weight = 60f,
                    restSeconds = 90
                )
            )
        )

        _workouts[wIndex] = workout.copy(
            exercises = workout.exercises + newExercise
        )
    }

    fun deleteExercise(workoutId: Int, exerciseId: Int) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]

        _workouts[wIndex] = workout.copy(
            exercises = workout.exercises.filterNot { it.id == exerciseId }
        )
    }

    fun updateSet(
        workoutId: Int,
        exerciseId: Int,
        setIndex: Int,
        newWeight: Float
    ) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]
        val exercise = workout.exercises.find { it.id == exerciseId } ?: return

        val updatedSets = exercise.sets.mapIndexed { index, set ->
            if (index == setIndex) {
                set.copy(weight = newWeight.coerceAtLeast(0f))
            } else set
        }

        val updatedExercises = workout.exercises.map {
            if (it.id == exerciseId) it.copy(sets = updatedSets) else it
        }

        _workouts[wIndex] = workout.copy(exercises = updatedExercises)
    }
    fun updateRepRange(
        workoutId: Int,
        exerciseId: Int,
        setIndex: Int,
        minReps: Int,
        maxReps: Int
    ) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]
        val exercise = workout.exercises.find { it.id == exerciseId } ?: return

        val updatedSets = exercise.sets.mapIndexed { index, set ->
            if (index == setIndex) {
                set.copy(
                    minReps = minReps.coerceAtLeast(0),
                    maxReps = maxReps.coerceAtLeast(minReps)
                )
            } else set
        }

        val updatedExercises = workout.exercises.map {
            if (it.id == exerciseId) it.copy(sets = updatedSets) else it
        }

        _workouts[wIndex] = workout.copy(exercises = updatedExercises)
    }
    fun addSet(
        workoutId: Int,
        exerciseId: Int
    ) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]
        val exercise = workout.exercises.find { it.id == exerciseId } ?: return

        val newSet = WorkoutSet(
            id = exercise.sets.size + 1,
            minReps = 8,
            maxReps = 12,
            weight = 20f,
            restSeconds = 90
        )



        val updatedExercises = workout.exercises.map {
            if (it.id == exerciseId)
                it.copy(sets = it.sets + newSet)
            else it
        }

        _workouts[wIndex] = workout.copy(exercises = updatedExercises)
    }

    fun deleteSet(
        workoutId: Int,
        exerciseId: Int,
        setIndex: Int
    ) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]
        val exercise = workout.exercises.find { it.id == exerciseId } ?: return

        val updatedSets = exercise.sets.filterIndexed { index, _ ->
            index != setIndex
        }

        val updatedExercises = workout.exercises.map {
            if (it.id == exerciseId)
                it.copy(sets = updatedSets)
            else it
        }

        _workouts[wIndex] = workout.copy(exercises = updatedExercises)
    }
    fun updateRest(
        workoutId: Int,
        exerciseId: Int,
        setIndex: Int,
        newRestSeconds: Int
    ) {
        val wIndex = _workouts.indexOfFirst { it.id == workoutId }
        if (wIndex == -1) return

        val workout = _workouts[wIndex]
        val exercise = workout.exercises.find { it.id == exerciseId } ?: return

        val updatedSets = exercise.sets.mapIndexed { index, set ->
            if (index == setIndex) {
                set.copy(restSeconds = newRestSeconds.coerceAtLeast(0))
            } else set
        }

        val updatedExercises = workout.exercises.map {
            if (it.id == exerciseId)
                it.copy(sets = updatedSets)
            else it
        }

        _workouts[wIndex] = workout.copy(exercises = updatedExercises)
    }
}
