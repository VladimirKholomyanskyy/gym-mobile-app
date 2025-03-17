package com.neyra.gymapp.domain.mapper

import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import com.neyra.gymapp.domain.model.TrainingProgram
import java.time.Instant
import java.util.UUID

/**
 * Converts a TrainingProgramEntity to a domain TrainingProgram
 * @return Mapped TrainingProgram domain model
 */
fun TrainingProgramEntity.toDomain(): TrainingProgram {
    return TrainingProgram(
        id = this.id,
        name = this.name,
        description = this.description.takeIf { it.isNotEmpty() },
        createdAt = Instant.ofEpochMilli(this.lastModified),
        workoutCount = 0 // Note: You might want to fetch actual workout count from repository
    )
}

/**
 * Converts a domain TrainingProgram to a TrainingProgramEntity
 * @param profileId The ID of the profile to associate the program with
 * @return Mapped TrainingProgramEntity for data layer
 */
fun TrainingProgram.toEntity(profileId: String): TrainingProgramEntity {
    return TrainingProgramEntity(
        id = this.id ?: UUID.randomUUID().toString(), // Generate UUID if not provided
        name = this.name,
        description = this.description ?: "", // Empty string if null
        profileId = profileId,
        syncStatus = SyncStatus.PENDING_CREATE, // Default sync status for new/modified programs
        lastModified = this.createdAt?.toEpochMilli() ?: System.currentTimeMillis()
    )
}

/**
 * Converts a list of TrainingProgramEntities to domain TrainingPrograms
 * @return List of mapped TrainingProgram domain models
 */
fun List<TrainingProgramEntity>.toDomainList(): List<TrainingProgram> {
    return map { it.toDomain() }
}

/**
 * Converts a list of domain TrainingPrograms to TrainingProgramEntities
 * @param profileId The ID of the profile to associate the programs with
 * @return List of mapped TrainingProgramEntities
 */
fun List<TrainingProgram>.toEntityList(profileId: String): List<TrainingProgramEntity> {
    return map { it.toEntity(profileId) }
}

/**
 * Safely updates an existing TrainingProgramEntity with domain model data
 * @param entity The existing entity to update
 * @param profileId The profile ID to use if needed
 * @return Updated TrainingProgramEntity
 */
fun TrainingProgram.updateEntity(
    entity: TrainingProgramEntity,
    profileId: String
): TrainingProgramEntity {
    return entity.copy(
        name = this.name,
        description = this.description ?: "",
        profileId = profileId,
        syncStatus = if (entity.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPDATE else entity.syncStatus,
        lastModified = System.currentTimeMillis()
    )
}