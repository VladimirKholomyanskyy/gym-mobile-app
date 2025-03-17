package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.WorkoutSessionEntity

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutSession: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE profileId = :profileId ORDER BY startTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllByProfileId(
        profileId: String,
        limit: Int,
        offset: Int
    ): List<WorkoutSessionEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE profileId = :profileId")
    suspend fun countByProfileId(profileId: String): Int

    @Query("UPDATE workout_sessions SET completionTime = :completionTime WHERE id = :id")
    suspend fun updateCompletionTime(id: String, completionTime: Long)

    @Delete
    suspend fun delete(workoutSession: WorkoutSessionEntity)
}