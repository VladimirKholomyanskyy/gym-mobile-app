package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingProgramDao {
    @Query("SELECT * FROM training_programs WHERE profileId = :profileId")
    fun getAllByProfileId(profileId: String): Flow<List<TrainingProgramEntity>>

    @Query("SELECT * FROM training_programs WHERE syncStatus IN (:statuses)")
    fun getAllWithSyncStatus(vararg statuses: SyncStatus): Flow<List<TrainingProgramEntity>>

    @Query("SELECT * FROM training_programs WHERE syncStatus IN (:statuses)")
    suspend fun getAllWithSyncStatusSync(vararg statuses: SyncStatus): List<TrainingProgramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: TrainingProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<TrainingProgramEntity>)

    // Update only the name
    @Query("UPDATE training_programs SET name = :name WHERE id = :id")
    suspend fun updateName(id: String, name: String): Int

    // Update only the description
    @Query("UPDATE training_programs SET description = :description WHERE id = :id")
    suspend fun updateDescription(id: String, description: String): Int

    // The original method that updates both fields
    @Query("UPDATE training_programs SET name = :name, description = :description WHERE id = :id")
    suspend fun updateNameAndDescription(id: String, name: String, description: String): Int

    @Query("UPDATE training_programs SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE training_programs SET id = :newId, syncStatus = :syncStatus, lastModified = :lastModified WHERE id = :oldId")
    suspend fun updateIdAndSyncStatus(
        oldId: String,
        newId: String,
        syncStatus: SyncStatus,
        lastModified: Long
    )

    @Query("SELECT * FROM training_programs WHERE id = :id")
    suspend fun getById(id: String): TrainingProgramEntity?


    @Query("DELETE FROM training_programs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM training_programs WHERE profileId = :profileId")
    suspend fun countByProfileId(profileId: String): Int
}