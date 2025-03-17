package com.neyra.gymapp.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "settings",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SettingsEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val language: String,
    val measurementSystem: String,  // Changed from LocaleData.MeasurementSystem
    val timeZone: String,  // Changed from TimeZone
    val notificationEnabled: Boolean
)