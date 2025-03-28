package com.neyra.gymapp.domain.model

import com.neyra.gymapp.data.entities.ExerciseEntity

data class Exercise(
    val id: String? = null,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val description: String
)

fun ExerciseEntity.toDomain(): Exercise {
    return Exercise(
        id = this.id,
        name = this.name,
        primaryMuscle = this.primaryMuscle,
        secondaryMuscles = this.secondaryMuscles,
        equipment = this.equipment,
        description = this.description,
    )
}