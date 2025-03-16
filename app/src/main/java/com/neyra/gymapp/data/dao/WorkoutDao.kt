package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutById(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE trainingProgramId = :trainingProgramId")
    fun getWorkoutsByTrainingProgramId(trainingProgramId: String): Flow<List<WorkoutEntity>>

    @Query("UPDATE workouts SET name = :name WHERE id = :id")
    suspend fun update(id: UUID, name: String)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun delete(id: String)


    @Delete
    suspend fun delete(workout: WorkoutEntity)

}