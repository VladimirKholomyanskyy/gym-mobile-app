package com.neyra.gymapp.domain.model

import java.time.Duration
import java.time.Instant

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
    val createdAt: Instant? = null,
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

    /**
     * Checks if this workout is considered intense based on exercise count and total sets
     * @return Boolean indicating if the workout is intense
     */
    fun isIntense(): Boolean {
        val totalSets = exercises.sumOf { it.sets }
        return exercises.size >= 5 || totalSets >= 20
    }

    /**
     * Calculates the primary muscle focus of this workout
     * @return Primary muscle group if there's a clear focus, null otherwise
     */
    fun getPrimaryMuscleGroup(): String? {
        // Group exercises by primary muscle and count
        val muscleGroups = exercises.groupBy { it.primaryMuscle }
            .mapValues { it.value.size }

        // Find the muscle with the most exercises
        val (muscle, count) = muscleGroups.maxByOrNull { it.value } ?: return null

        // Only return the muscle if it accounts for at least 40% of the exercises
        val threshold = exercises.size * 0.4
        return if (count >= threshold) muscle else null
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