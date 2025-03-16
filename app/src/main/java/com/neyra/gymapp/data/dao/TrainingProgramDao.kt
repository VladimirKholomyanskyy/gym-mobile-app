package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TrainingProgramDao {
    @Query("SELECT * FROM training_programs WHERE profileId = :profileId")
    fun getAllByProfileId(profileId: UUID): Flow<List<TrainingProgramEntity>>

    @Query("SELECT * FROM training_programs WHERE syncStatus IN (:statuses)")
    fun getAllWithSyncStatus(vararg statuses: SyncStatus): Flow<List<TrainingProgramEntity>>

    @Query("SELECT * FROM training_programs WHERE syncStatus IN (:statuses)")
    suspend fun getAllWithSyncStatusSync(vararg statuses: SyncStatus): List<TrainingProgramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(program: TrainingProgramEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<TrainingProgramEntity>)

    @Query("UPDATE training_programs SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: UUID, status: SyncStatus)

    @Query("UPDATE training_programs SET id = :newId, syncStatus = :syncStatus, lastModified = :lastModified WHERE id = :oldId")
    suspend fun updateIdAndSyncStatus(
        oldId: UUID,
        newId: UUID,
        syncStatus: SyncStatus,
        lastModified: Long
    )

    @Query("SELECT * FROM training_programs WHERE id = :id")
    suspend fun getById(id: UUID): TrainingProgramEntity?

    @Transaction
    suspend fun upsertAll(programs: List<TrainingProgramEntity>) {
        insertAll(programs)
    }

    @Query("DELETE FROM training_programs WHERE id = :id")
    suspend fun delete(id: UUID)

}