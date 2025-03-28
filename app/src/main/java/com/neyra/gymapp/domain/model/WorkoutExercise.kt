package com.neyra.gymapp.domain.model

/**
 * Domain model representing an Exercise within a Workout
 *
 * This is the junction between a Workout and an Exercise, containing
 * the specific configuration of an exercise within a workout (sets, reps, position)
 */
data class WorkoutExercise(
    val id: String? = null,
    val workoutId: String,
    val sets: Int,
    val reps: Int,
    val position: Int = 0,
    val exercise: Exercise

) {
    // Initialization block for validation
    init {
        // Validate sets and reps
        require(sets > 0) { "Sets must be greater than 0" }
        require(reps > 0) { "Reps must be greater than 0" }

        // Validate position
        require(position >= 0) { "Position cannot be negative" }
    }
}
