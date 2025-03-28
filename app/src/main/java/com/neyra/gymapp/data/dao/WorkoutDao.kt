package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.WorkoutEntity
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

    @Query("SELECT COUNT(*) FROM workouts WHERE trainingProgramId = :trainingProgramId")
    suspend fun countByTrainingProgramId(trainingProgramId: String): Int

    @Query("UPDATE workouts SET name = :name, syncStatus = :syncStatus, localUpdatedAt = :localUpdatedAt WHERE id = :id")
    suspend fun updateName(
        id: String,
        name: String,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE,
        localUpdatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE workouts SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE workouts SET position = :position, syncStatus = :syncStatus, localUpdatedAt = :localUpdatedAt WHERE id = :id")
    suspend fun updatePosition(
        id: String, position: Int, syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE,
        localUpdatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE workouts SET syncStatus = :status, serverUpdatedAt = :serverUpdatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus, serverUpdatedAt: Long)
    
    @Query("UPDATE workouts SET id = :newId, syncStatus = :syncStatus,serverCreatedAt = :serverCreatedAt, serverUpdatedAt = :serverUpdatedAt WHERE id = :oldId")
    suspend fun updateIdAndSyncStatus(
        oldId: String,
        newId: String,
        syncStatus: SyncStatus,
        serverCreatedAt: Long,
        serverUpdatedAt: Long
    )

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
        SET position = position - 1,
        syncStatus = :syncStatus, localUpdatedAt = :localUpdatedAt
        WHERE trainingProgramId = :programId
        AND position BETWEEN :start AND :end
    """
    )
    suspend fun decrementPositions(
        programId: String, start: Int, end: Int, syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE,
        localUpdatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE workouts 
        SET position = position + 1,
        syncStatus = :syncStatus,
        localUpdatedAt = :localUpdatedAt
        WHERE trainingProgramId = :programId
        AND position BETWEEN :start AND :end
    """
    )
    suspend fun incrementPositions(
        programId: String,
        start: Int,
        end: Int,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPDATE,
        localUpdatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: String)


    @Query("SELECT MAX(position) FROM workouts WHERE trainingProgramId = :programId")
    suspend fun getMaxPosition(programId: String): Int?
}