package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.ScheduledWorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheduledWorkout: ScheduledWorkoutEntity)

    @Query("SELECT * FROM scheduled_workouts WHERE id = :id")
    suspend fun getById(id: String): ScheduledWorkoutEntity?

    @Query("SELECT * FROM scheduled_workouts WHERE profileId = :profileId ORDER BY date ASC")
    fun getAllByProfileId(profileId: String): Flow<List<ScheduledWorkoutEntity>>

    @Query("SELECT * FROM scheduled_workouts WHERE profileId = :profileId ORDER BY date ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllByProfileIdPaginated(
        profileId: String,
        limit: Int,
        offset: Int
    ): List<ScheduledWorkoutEntity>

    @Query("SELECT * FROM scheduled_workouts WHERE profileId = :profileId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC LIMIT :limit OFFSET :offset")
    suspend fun getAllByProfileIdAndDateRange(
        profileId: String,
        startDate: Long,
        endDate: Long,
        limit: Int,
        offset: Int
    ): List<ScheduledWorkoutEntity>

    @Query("SELECT * FROM scheduled_workouts WHERE profileId = :profileId AND date >= :date ORDER BY date ASC LIMIT 1")
    suspend fun getNextScheduledWorkout(profileId: String, date: Long): ScheduledWorkoutEntity?

    @Query("UPDATE scheduled_workouts SET date = :date, notes = :notes WHERE id = :id")
    suspend fun update(id: String, date: Long, notes: String?)

    @Delete
    suspend fun delete(scheduledWorkout: ScheduledWorkoutEntity)

    @Query("DELETE FROM scheduled_workouts WHERE id = :id")
    suspend fun deleteById(id: String)
}