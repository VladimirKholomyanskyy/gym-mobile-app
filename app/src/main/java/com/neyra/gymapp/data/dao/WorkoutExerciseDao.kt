package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutExercise: WorkoutExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workoutExercises: List<WorkoutExerciseEntity>)

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getById(id: String): WorkoutExerciseEntity?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position ASC")
    fun getAllByWorkoutId(workoutId: String): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllByWorkoutIdPaginated(
        workoutId: String,
        limit: Int,
        offset: Int
    ): List<WorkoutExerciseEntity>

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun countByWorkoutId(workoutId: String): Int

    @Query("UPDATE workout_exercises SET position = :newPosition WHERE id = :id")
    suspend fun updatePosition(id: String, newPosition: Int)

    @Delete
    suspend fun delete(workoutExercise: WorkoutExerciseEntity)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteById(id: String)
}