package com.neyra.gymapp.data.mapper

import com.neyra.gymapp.data.entities.ProfileEntity
import com.neyra.gymapp.openapi.models.Profile

fun Profile.toEntity(): ProfileEntity {
    return ProfileEntity(
        id = this.id,
        sex = this.sex,
        height = this.height,
        weight = this.weight,
        birthday = this.birthday?.toEpochDay(),
        avatarUrl = this.avatarUrl
    )
}