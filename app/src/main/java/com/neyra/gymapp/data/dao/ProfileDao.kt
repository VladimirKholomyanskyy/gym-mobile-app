package com.neyra.gymapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.openapi.models.Sex
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfile(profileId: String): ProfileEntity?

    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE externalId = :externalId LIMIT 1")
    suspend fun getProfileByExternalId(externalId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :profileId")
    suspend fun setDefaultProfile(profileId: String)

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: String)

    @Query(
        """
        UPDATE profiles 
        SET 
            sex = COALESCE(:sex, sex), 
            birthday = COALESCE(:birthday, birthday), 
            height = COALESCE(:height, height), 
            weight = COALESCE(:weight, weight), 
            avatarUrl = COALESCE(:avatarUrl, avatarUrl)
        WHERE id = :id
    """
    )
    suspend fun updateProfile(
        id: UUID,
        sex: Sex?,
        birthday: Long?,
        height: Double?,
        weight: Double?,
        avatarUrl: String?
    )
}