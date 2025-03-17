package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neyra.gymapp.data.entities.WorkoutEntity
import com.neyra.gymapp.data.entities.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<WorkoutEntity>)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE trainingProgramId = :trainingProgramId ORDER BY position ASC")
    fun getWorkoutsByTrainingProgramId(trainingProgramId: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId IN (:workoutIds) ORDER BY workoutId, position ASC")
    fun getAllByWorkoutIds(workoutIds: List<String>): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT COUNT(*) FROM workouts WHERE trainingProgramId = :trainingProgramId")
    suspend fun countByTrainingProgramId(trainingProgramId: String): Int

    @Query("UPDATE workouts SET name = :name WHERE id = :id")
    suspend fun update(id: String, name: String)

    @Query("UPDATE workouts SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Transaction
    suspend fun reorderWorkouts(workoutId: String, newPosition: Int) {
        val workout = getWorkoutById(workoutId) ?: return
        val currentPosition = workout.position

        if (currentPosition == newPosition) return

        if (currentPosition < newPosition) {
            // Moving down: decrement positions of workouts between current and new
            decrementPositions(workout.trainingProgramId, currentPosition + 1, newPosition)
        } else {
            // Moving up: increment positions of workouts between new and current
            incrementPositions(workout.trainingProgramId, newPosition, currentPosition - 1)
        }

        // Set the new position for this workout
        updatePosition(workoutId, newPosition)
    }

    @Query(
        """
        UPDATE workouts 
        SET position = position - 1 
        WHERE trainingProgramId = :programId 
        AND position BETWEEN :start AND :end
    """
    )
    suspend fun decrementPositions(programId: String, start: Int, end: Int)

    @Query(
        """
        UPDATE workouts 
        SET position = position + 1 
        WHERE trainingProgramId = :programId 
        AND position BETWEEN :start AND :end
    """
    )
    suspend fun incrementPositions(programId: String, start: Int, end: Int)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: String)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("SELECT MAX(position) FROM workouts WHERE trainingProgramId = :programId")
    suspend fun getMaxPosition(programId: String): Int?
}