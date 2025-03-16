package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neyra.gymapp.openapi.models.Sex
import java.util.UUID

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: UUID,
    val externalId: String,
    val sex: Sex?,
    val birthday: Long?,
    val height: Double?,
    val weight: Double?,
    val avatarUrl: String?,
    val lastSynced: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)