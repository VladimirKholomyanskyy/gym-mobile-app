package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.ExerciseEntity

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE name LIKE :query OR primaryMuscle LIKE :query")
    suspend fun searchExercisesByName(query: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE primaryMuscle = :muscleGroup")
    suspend fun getExercisesByMuscleGroup(muscleGroup: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment")
    suspend fun getExercisesByEquipment(equipment: String): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}