    package com.example.gymtrackerphone.data.repository

    import com.example.gymtrackerphone.data.dao.WorkoutDao
    import com.example.gymtrackerphone.data.entity.*
    import com.example.gymtrackerphone.data.mapper.toUi
    import com.example.gymtrackerphone.data.model.WorkoutUi
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.map

    class WorkoutRepository(
        private val dao: WorkoutDao
    ) {

        val workouts: Flow<List<WorkoutUi>> =
            dao.getWorkoutsWithExercises()
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

        suspend fun updateWorkout(workoutId: Int, name: String) {
            dao.updateWorkout(workoutId, name)
        }

        suspend fun deleteWorkout(workoutId: Int) {
            dao.deleteWorkoutById(workoutId)
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
            dao.insertSet(
                WorkoutSetEntity(
                    exerciseId = exerciseId,
                    minReps = 8,
                    maxReps = 12,
                    weight = 20f,
                    restSeconds = 90
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
    }