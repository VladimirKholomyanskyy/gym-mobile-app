package com.neyra.gymapp.domain.model

import java.time.Duration

/**
 * Domain model representing a Workout within a Training Program
 *
 * Key characteristics:
 * - Immutable data class
 * - Includes validation logic
 * - Provides domain-specific methods
 */
data class Workout(
    val id: String? = null,  // Optional for new workouts
    val trainingProgramId: String,
    val name: String,
    val position: Int = 0,
    val exercises: List<WorkoutExercise> = emptyList()
) {
    // Initialization block for validation
    init {
        // Validate name
        require(name.isNotBlank()) { "Workout name cannot be empty" }
        require(name.length <= MAX_NAME_LENGTH) {
            "Workout name cannot exceed $MAX_NAME_LENGTH characters"
        }

        // Validate position
        require(position >= 0) { "Position cannot be negative" }
    }

    /**
     * Calculates the estimated duration of the workout
     * @return Duration of the workout
     */
    fun estimateDuration(): Duration {
        // Basic estimate: 3 minutes per set across all exercises
        val totalSets = exercises.sumOf { it.sets }
        return Duration.ofMinutes((totalSets * 3).toLong())
    }


    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_EXERCISES = 15
    }
}

// Extension function to check if a workout has exercises
fun Workout.hasExercises(): Boolean = exercises.isNotEmpty()

// Extension function to check if the workout can add more exercises
fun Workout.canAddExercises(): Boolean = exercises.size < Workout.MAX_EXERCISES