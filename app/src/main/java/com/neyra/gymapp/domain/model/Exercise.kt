package com.neyra.gymapp.domain.model

data class Exercise(
    val id: String? = null,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val description: String
)
