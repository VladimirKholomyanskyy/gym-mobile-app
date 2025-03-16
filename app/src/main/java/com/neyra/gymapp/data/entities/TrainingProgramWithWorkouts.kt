package com.neyra.gymapp.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class TrainingProgramWithWorkouts(
    @Embedded val trainingProgram: TrainingProgramEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "trainingProgramId"
    )
    val exercises: List<WorkoutEntity>
)
