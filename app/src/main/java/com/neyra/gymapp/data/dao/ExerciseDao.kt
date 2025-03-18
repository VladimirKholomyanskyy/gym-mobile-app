package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.neyra.gymapp.data.entities.ExerciseEntity

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<ExerciseEntity>


}