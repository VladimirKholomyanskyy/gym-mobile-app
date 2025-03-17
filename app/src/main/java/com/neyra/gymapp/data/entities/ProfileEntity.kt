package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val externalId: String,
    val sex: String?,  // Will use converter for Sex enum
    val birthday: Long?, // Store as timestamp
    val height: Double?,
    val weight: Double?,
    val avatarUrl: String?,
    val isDefault: Boolean = false
)