package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.ExerciseLogEntity

@Dao
interface ExerciseLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exerciseLog: ExerciseLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exerciseLogs: List<ExerciseLogEntity>)

    @Query("SELECT * FROM exercise_logs WHERE id = :id")
    suspend fun getById(id: String): ExerciseLogEntity?

    @Query("SELECT * FROM exercise_logs WHERE profileId = :profileId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllByProfileId(
        profileId: String,
        limit: Int,
        offset: Int
    ): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_logs WHERE profileId = :profileId AND sessionId = :sessionId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllByProfileIdAndSessionId(
        profileId: String,
        sessionId: String,
        limit: Int,
        offset: Int
    ): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_logs WHERE profileId = :profileId AND exerciseId = :exerciseId ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllByProfileIdAndExerciseId(
        profileId: String,
        exerciseId: String,
        limit: Int,
        offset: Int
    ): List<ExerciseLogEntity>

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE profileId = :profileId")
    suspend fun countByProfileId(profileId: String): Int

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE profileId = :profileId AND sessionId = :sessionId")
    suspend fun countByProfileIdAndSessionId(profileId: String, sessionId: String): Int

    @Query("SELECT COUNT(*) FROM exercise_logs WHERE profileId = :profileId AND exerciseId = :exerciseId")
    suspend fun countByProfileIdAndExerciseId(profileId: String, exerciseId: String): Int

    @Delete
    suspend fun delete(exerciseLog: ExerciseLogEntity)
}