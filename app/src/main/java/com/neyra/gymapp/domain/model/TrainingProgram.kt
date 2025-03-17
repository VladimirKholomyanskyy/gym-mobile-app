package com.neyra.gymapp.domain.model

import com.neyra.gymapp.data.entities.SyncStatus
import com.neyra.gymapp.data.entities.TrainingProgramEntity
import java.time.Instant
import java.util.UUID

data class TrainingProgram(
    val id: String? = null,  // Optional for new programs
    val name: String,
    val description: String? = null,
    val createdAt: Instant? = null,
    val workoutCount: Int = 0
) {
    init {
        require(name.isNotBlank()) { "Training program name cannot be empty" }
        require(name.length <= 50) { "Training program name cannot exceed 50 characters" }
        description?.let {
            require(it.length <= 500) { "Description cannot exceed 500 characters" }
        }
    }

    // You can add domain-specific methods here
    fun canAddMoreWorkouts(): Boolean {
        return workoutCount < MAX_WORKOUTS
    }

    companion object {
        const val MAX_WORKOUTS = 10
    }
}

// Mapper to convert between domain and data layer objects
fun TrainingProgramEntity.toDomain(): TrainingProgram {
    return TrainingProgram(
        id = this.id,
        name = this.name,
        description = this.description,
        createdAt = Instant.ofEpochMilli(this.lastModified)
    )
}

fun TrainingProgram.toEntity(profileId: String): TrainingProgramEntity {
    return TrainingProgramEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        name = this.name,
        description = this.description ?: "",
        profileId = profileId,
        syncStatus = SyncStatus.PENDING_CREATE,
        lastModified = System.currentTimeMillis()
    )
}
