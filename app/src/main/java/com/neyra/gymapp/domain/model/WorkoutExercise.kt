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
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val position: Int = 0,
    val primaryMuscle: String = "",  // Cached from the Exercise entity for convenience
    val exerciseName: String = ""    // Cached from the Exercise entity for convenience
) {
    // Initialization block for validation
    init {
        // Validate sets and reps
        require(sets > 0) { "Sets must be greater than 0" }
        require(reps > 0) { "Reps must be greater than 0" }

        // Validate position
        require(position >= 0) { "Position cannot be negative" }
    }

    /**
     * Calculate the total repetitions for this exercise
     * @return Total number of repetitions
     */
    fun totalRepetitions(): Int = sets * reps

    /**
     * Check if this is a high-volume exercise configuration
     * @return True if high volume (many total reps), false otherwise
     */
    fun isHighVolume(): Boolean = totalRepetitions() >= HIGH_VOLUME_THRESHOLD

    /**
     * Creates a copy with updated position
     * @param newPosition The new position in the workout
     * @return Updated WorkoutExercise
     */
    fun withPosition(newPosition: Int): WorkoutExercise {
        require(newPosition >= 0) { "Position cannot be negative" }
        return copy(position = newPosition)
    }

    companion object {
        const val HIGH_VOLUME_THRESHOLD = 50  // 5 sets of 10 reps or equivalent

        /**
         * Factory method to create a WorkoutExercise with minimal information
         */
        fun create(
            workoutId: String,
            exerciseId: String,
            sets: Int,
            reps: Int,
            exerciseName: String = "",
            primaryMuscle: String = ""
        ): WorkoutExercise {
            return WorkoutExercise(
                workoutId = workoutId,
                exerciseId = exerciseId,
                sets = sets,
                reps = reps,
                exerciseName = exerciseName,
                primaryMuscle = primaryMuscle
            )
        }
    }
}

// Extension function to calculate volume (sets * reps)
fun WorkoutExercise.volume(): Int = sets * reps