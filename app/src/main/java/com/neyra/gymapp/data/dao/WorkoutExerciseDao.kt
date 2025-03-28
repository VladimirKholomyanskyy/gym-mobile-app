package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neyra.gymapp.data.entities.SyncStatus
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

    @Query("SELECT * FROM workout_exercises WHERE workoutId IN (:workoutIds) ORDER BY workoutId, position ASC")
    fun getAllByWorkoutIds(workoutIds: List<String>): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllByWorkoutIdPaginated(
        workoutId: String,
        limit: Int,
        offset: Int
    ): List<WorkoutExerciseEntity>

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun countByWorkoutId(workoutId: String): Int

    @Query("UPDATE workout_exercises SET sets = :sets, reps = :reps, syncStatus = :syncStatus, position = :position WHERE id = :id")
    suspend fun update(
        id: String,
        sets: Int,
        reps: Int,
        position: Int,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE
    ): Int

    @Query("UPDATE workout_exercises SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus): Int

    @Query("UPDATE workout_exercises SET position = :newPosition, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updatePosition(
        id: String,
        newPosition: Int,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE
    ): Int

    @Query("UPDATE workout_exercises SET id = :newId, syncStatus = :syncStatus WHERE id = :oldId")
    suspend fun updateIdAndSyncStatus(
        oldId: String,
        newId: String,
        syncStatus: SyncStatus
    ): Int

    @Transaction
    suspend fun reorderExercises(exerciseId: String, newPosition: Int) {
        val exercise = getById(exerciseId) ?: return
        val currentPosition = exercise.position

        if (currentPosition == newPosition) return

        if (currentPosition < newPosition) {
            // Moving down: decrement positions of exercises between current and new
            decrementPositions(exercise.workoutId, currentPosition + 1, newPosition)
        } else {
            // Moving up: increment positions of exercises between new and current
            incrementPositions(exercise.workoutId, newPosition, currentPosition - 1)
        }

        // Set the new position for this exercise
        updatePosition(exerciseId, newPosition)
    }

    @Query(
        """
        UPDATE workout_exercises 
        SET position = position - 1,
        syncStatus = :syncStatus
        WHERE workoutId = :workoutId 
        AND position BETWEEN :start AND :end
    """
    )
    suspend fun decrementPositions(
        workoutId: String,
        start: Int,
        end: Int,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE
    ): Int

    @Query(
        """
        UPDATE workout_exercises 
        SET position = position + 1,
        syncStatus = :syncStatus
        WHERE workoutId = :workoutId 
        AND position BETWEEN :start AND :end
    """
    )
    suspend fun incrementPositions(
        workoutId: String,
        start: Int,
        end: Int,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE
    ): Int

    @Delete
    suspend fun delete(workoutExercise: WorkoutExerciseEntity): Int

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT MAX(position) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getMaxPosition(workoutId: String): Int?
}