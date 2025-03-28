package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = :id")
    suspend fun getSettings(id: String): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE id = :id")
    fun observeSettings(id: String): Flow<SettingsEntity?>

//    @Query(
//        """
//        UPDATE settings
//        SET
//            language = COALESCE(:language, language),
//            measurementSystem = COALESCE(:measurementSystem, measurementSystem),
//            timeZone = COALESCE(:timeZone, timeZone),
//            notificationEnabled = COALESCE(:notificationEnabled, notificationEnabled)
//        WHERE id = :id
//    """
//    )
//    suspend fun updateSettings(
//        id: String,
//        language: String?,
//        measurementSystem: LocaleData.MeasurementSystem?,
//        timeZone: TimeZone?,
//        notificationEnabled: Boolean?
//    )
}