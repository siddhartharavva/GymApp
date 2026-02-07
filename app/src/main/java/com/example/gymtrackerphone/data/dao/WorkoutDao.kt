package com.example.gymtrackerphone.data.dao

import androidx.room.*
import com.example.gymtrackerphone.data.entity.*
import com.example.gymtrackerphone.data.relation.CompletedWorkoutWithExercises
import com.example.gymtrackerphone.data.relation.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // ---------- WORKOUTS ----------

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET name = :name WHERE id = :workoutId")
    suspend fun updateWorkout(workoutId: Int, name: String)

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Int): WorkoutEntity

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId")
    suspend fun getExercisesForWorkout(workoutId: Int): List<ExerciseEntity>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId")
    suspend fun getSetsForExercise(exerciseId: Int): List<WorkoutSetEntity>

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Int)

    // ---------- EXERCISES ----------
    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity)


    @Transaction
    @Query("SELECT * FROM workouts")
    fun getWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>


    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Int)



    // ---------- SETS ----------
    @Insert
    suspend fun insertSet(set: WorkoutSetEntity)

    @Query("DELETE FROM sets WHERE id = :setId")
    suspend fun deleteSetById(setId: Int)

    @Query("UPDATE sets SET minReps = :min, maxReps = :max WHERE id = :setId")
    suspend fun updateRepRange(setId: Int, min: Int, max: Int)

    @Query("UPDATE sets SET weight = :weight WHERE id = :setId")
    suspend fun updateWeight(setId: Int, weight: Float)

    @Query("UPDATE sets SET restSeconds = :rest WHERE id = :setId")
    suspend fun updateRest(setId: Int, rest: Int)

    // ---------- COMPLETED WORKOUTS ----------

    @Insert
    suspend fun insertCompletedWorkout(workout: CompletedWorkoutEntity): Long

    @Insert
    suspend fun insertCompletedExercise(exercise: CompletedExerciseEntity): Long

    @Insert
    suspend fun insertCompletedSets(sets: List<CompletedSetEntity>)

    @Transaction
    @Query("SELECT * FROM completed_workouts ORDER BY completedAtEpochMs DESC")
    fun getCompletedWorkouts(): Flow<List<CompletedWorkoutWithExercises>>

    @Transaction
    @Query(
        "SELECT * FROM completed_workouts " +
            "WHERE templateWorkoutId = :templateId " +
            "ORDER BY completedAtEpochMs DESC " +
            "LIMIT :limit"
    )
    suspend fun getRecentCompletedWorkouts(
        templateId: Int,
        limit: Int
    ): List<CompletedWorkoutWithExercises>
}
