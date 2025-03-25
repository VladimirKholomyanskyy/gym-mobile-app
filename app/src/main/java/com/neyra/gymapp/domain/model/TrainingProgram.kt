package com.neyra.gymapp.domain.model

/**
 * Domain model representing a Training Program
 *
 * Key characteristics:
 * - Immutable data class
 * - Includes validation logic
 * - Provides domain-specific methods
 */
data class TrainingProgram(
    val id: String? = null,  // Optional for new programs
    val name: String,
    val description: String? = null,
    val workoutCount: Int = 0
) {
    // Initialization block for validation
    init {
        // Validate name
        require(name.isNotBlank()) { "Training program name cannot be empty" }
        require(name.length <= MAX_NAME_LENGTH) {
            "Training program name cannot exceed $MAX_NAME_LENGTH characters"
        }

        // Optional description validation
        description?.let {
            require(it.length <= MAX_DESCRIPTION_LENGTH) {
                "Description cannot exceed $MAX_DESCRIPTION_LENGTH characters"
            }
        }
    }

    // Domain-specific business logic methods

    /**
     * Checks if more workouts can be added to this training program
     * @return Boolean indicating if more workouts can be added
     */
    fun canAddMoreWorkouts(): Boolean {
        return workoutCount < MAX_WORKOUTS
    }

    /**
     * Creates a new training program with an updated workout count
     * @param additionalWorkouts Number of workouts to add
     * @return Updated TrainingProgram instance
     * @throws IllegalArgumentException if adding workouts would exceed max limit
     */
    fun addWorkouts(additionalWorkouts: Int): TrainingProgram {
        val newWorkoutCount = workoutCount + additionalWorkouts
        require(newWorkoutCount <= MAX_WORKOUTS) {
            "Cannot add $additionalWorkouts workouts. Maximum allowed is $MAX_WORKOUTS"
        }
        return copy(workoutCount = newWorkoutCount)
    }

    /**
     * Determines the complexity of the training program based on workout count
     * @return Complexity level of the training program
     */
    fun getProgramComplexity(): ProgramComplexity {
        return when {
            workoutCount <= 2 -> ProgramComplexity.BEGINNER
            workoutCount <= 4 -> ProgramComplexity.INTERMEDIATE
            else -> ProgramComplexity.ADVANCED
        }
    }

    // Companion object for constants and potentially factory methods
    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_DESCRIPTION_LENGTH = 500
        const val MAX_WORKOUTS = 10
    }

    // Enum to represent program complexity
    enum class ProgramComplexity {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
}

// Extension function to check if a training program is empty
fun TrainingProgram.isEmpty(): Boolean {
    return workoutCount == 0
}

// Extension function to get a descriptive string for program complexity
fun TrainingProgram.ProgramComplexity.getDescription(): String {
    return when (this) {
        TrainingProgram.ProgramComplexity.BEGINNER -> "Suitable for those new to fitness"
        TrainingProgram.ProgramComplexity.INTERMEDIATE -> "Balanced workout program"
        TrainingProgram.ProgramComplexity.ADVANCED -> "Intense training for experienced individuals"
    }
}