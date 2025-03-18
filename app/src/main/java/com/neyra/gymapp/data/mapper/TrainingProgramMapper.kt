package com.neyra.gymapp.data.mapper

import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.openapi.models.TrainingProgram
import java.util.UUID

// Convert entity to API model
fun TrainingProgramEntity.toModel(): TrainingProgram {
    return TrainingProgram(
        id = UUID.fromString(this.id),
        name = this.name,
        profileId = UUID.fromString(this.profileId),
        description = this.description.ifEmpty { "" }
    )
}

// Convert list of entities to list of API models
fun List<TrainingProgramEntity>.toModels(): List<TrainingProgram> {
    return this.map { it.toModel() }
}

// Convert API model to entity
fun TrainingProgram.toEntity(profileId: UUID): TrainingProgramEntity {
    return TrainingProgramEntity(
        id = this.id.toString(),
        name = this.name,
        description = this.description ?: "",
        profileId = profileId.toString(),
        syncStatus = SyncStatus.SYNCED,
        lastModified = System.currentTimeMillis()
    )
}