package com.neyra.gymapp.data.entities

import android.icu.util.LocaleData
import android.icu.util.TimeZone
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: UUID,
    val profileId: UUID,
    val language: String,
    val measurementSystem: LocaleData.MeasurementSystem,
    val timeZone: TimeZone,
    val notificationEnabled: Boolean,
)
