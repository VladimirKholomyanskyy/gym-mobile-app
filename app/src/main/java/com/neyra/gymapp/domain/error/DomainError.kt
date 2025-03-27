package com.neyra.gymapp.domain.error

/**
 * Sealed class representing domain-specific errors within the application.
 * This hierarchical structure allows for precise error handling and mapping
 * across the entire domain layer.
 */
sealed class DomainError(
    open val code: String,
    override val message: String,
    override val cause: Exception? = null,
    open val context: Map<String, Any> = emptyMap()
) : Exception(message, cause) {

    /**
     * Creates a copy of this error with additional context information
     */
    abstract fun withContext(key: String, value: Any): DomainError

    /**
     * Authentication and Authorization Errors
     * Represent issues with user authentication state or permissions
     */
    sealed class AuthenticationError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class Unauthorized(
            override val context: Map<String, Any> = emptyMap()
        ) : AuthenticationError(
            code = "AUTH001",
            message = "User is not authenticated",
            context = context
        ) {
            override fun withContext(key: String, value: Any): Unauthorized {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = Unauthorized()
            }
        }

        data class Forbidden(
            override val context: Map<String, Any> = emptyMap()
        ) : AuthenticationError(
            code = "AUTH002",
            message = "User does not have permission",
            context = context
        ) {
            override fun withContext(key: String, value: Any): Forbidden {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = Forbidden()
            }
        }

        data class TokenExpired(
            override val message: String = "Authentication token has expired",
            override val cause: Exception? = null,
            override val context: Map<String, Any> = emptyMap()
        ) : AuthenticationError(
            code = "AUTH003",
            message = message,
            cause = cause,
            context = context
        ) {
            override fun withContext(key: String, value: Any): TokenExpired {
                return copy(context = context + (key to value))
            }
        }
    }

    /**
     * Network-related Errors
     * Represent issues with network connectivity or responses
     */
    sealed class NetworkError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class NoConnection(
            override val context: Map<String, Any> = emptyMap()
        ) : NetworkError(
            code = "NET001",
            message = "No internet connection",
            context = context
        ) {
            override fun withContext(key: String, value: Any): NoConnection {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = NoConnection()
            }
        }

        data class Timeout(
            override val context: Map<String, Any> = emptyMap()
        ) : NetworkError(
            code = "NET002",
            message = "Network request timed out",
            context = context
        ) {
            override fun withContext(key: String, value: Any): Timeout {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = Timeout()
            }
        }

        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error occurred",
            override val cause: Exception? = null,
            override val context: Map<String, Any> = emptyMap()
        ) : NetworkError(
            code = "NET003",
            message = message,
            cause = cause,
            context = context
        ) {
            override fun withContext(key: String, value: Any): ServerError {
                return copy(context = context + (key to value))
            }
        }
    }

    /**
     * Validation Errors
     * Represent issues with data validation within the domain
     */
    sealed class ValidationError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class InvalidName(
            val name: String,
            override val message: String = "Invalid name: $name",
            override val context: Map<String, Any> = emptyMap()
        ) : ValidationError(
            code = "VAL001",
            message = message,
            context = context
        ) {
            override fun withContext(key: String, value: Any): InvalidName {
                return copy(context = context + (key to value))
            }
        }

        data class InvalidDescription(
            val description: String,
            override val message: String = "Invalid description",
            override val context: Map<String, Any> = emptyMap()
        ) : ValidationError(
            code = "VAL002",
            message = message,
            context = context
        ) {
            override fun withContext(key: String, value: Any): InvalidDescription {
                return copy(context = context + (key to value))
            }
        }

        data class WorkoutLimitExceeded(
            override val context: Map<String, Any> = emptyMap()
        ) : ValidationError(
            code = "VAL003",
            message = "Maximum number of workouts exceeded",
            context = context
        ) {
            override fun withContext(key: String, value: Any): WorkoutLimitExceeded {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = WorkoutLimitExceeded()
            }
        }
    }

    /**
     * Data-related Errors
     * Represent issues with data access or state
     */
    sealed class DataError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class NotFound(
            override val context: Map<String, Any> = emptyMap()
        ) : DataError(
            code = "DATA001",
            message = "Requested resource not found",
            context = context
        ) {
            override fun withContext(key: String, value: Any): NotFound {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = NotFound()
            }
        }

        data class DuplicateEntry(
            val identifier: String,
            override val message: String = "Duplicate entry exists",
            override val context: Map<String, Any> = emptyMap()
        ) : DataError(
            code = "DATA002",
            message = message,
            context = context
        ) {
            override fun withContext(key: String, value: Any): DuplicateEntry {
                return copy(context = context + (key to value))
            }
        }

        data class DatabaseError(
            override val context: Map<String, Any> = emptyMap()
        ) : DataError(
            code = "DATA003",
            message = "Database operation failed",
            context = context
        ) {
            override fun withContext(key: String, value: Any): DatabaseError {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = DatabaseError()
            }
        }
    }

    /**
     * Synchronization Errors
     * Represent issues with data synchronization between local and remote sources
     */
    sealed class SyncError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class PartialSync(
            override val context: Map<String, Any> = emptyMap()
        ) : SyncError(
            code = "SYNC001",
            message = "Some items were not synchronized",
            context = context
        ) {
            override fun withContext(key: String, value: Any): PartialSync {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = PartialSync()
            }
        }

        data class SyncFailed(
            override val context: Map<String, Any> = emptyMap()
        ) : SyncError(
            code = "SYNC002",
            message = "Synchronization failed",
            context = context
        ) {
            override fun withContext(key: String, value: Any): SyncFailed {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = SyncFailed()
            }
        }
    }

    /**
     * Workout-specific Errors
     * Represent domain-specific issues related to workouts
     */
    sealed class WorkoutError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class ExerciseNotFound(
            val exerciseId: String,
            override val context: Map<String, Any> = emptyMap()
        ) : WorkoutError(
            code = "WORKOUT001",
            message = "Exercise with ID $exerciseId not found",
            context = context
        ) {
            override fun withContext(key: String, value: Any): ExerciseNotFound {
                return copy(context = context + (key to value))
            }
        }

        data class MaxExercisesReached(
            val workoutId: String,
            override val context: Map<String, Any> = emptyMap()
        ) : WorkoutError(
            code = "WORKOUT002",
            message = "Workout has reached maximum number of exercises",
            context = context
        ) {
            override fun withContext(key: String, value: Any): MaxExercisesReached {
                return copy(context = context + (key to value))
            }
        }

        data class InvalidSetsOrReps(
            val value: Int,
            override val context: Map<String, Any> = emptyMap()
        ) : WorkoutError(
            code = "WORKOUT003",
            message = "Invalid number of sets/reps: $value",
            context = context
        ) {
            override fun withContext(key: String, value: Any): InvalidSetsOrReps {
                return copy(context = context + (key to value))
            }
        }
    }

    /**
     * User-specific Errors
     * Represent domain-specific issues related to users
     */
    sealed class UserError(
        override val code: String,
        override val message: String,
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(code, message, cause, context) {

        data class ProfileNotFound(
            val userId: String,
            override val context: Map<String, Any> = emptyMap()
        ) : UserError(
            code = "USER001",
            message = "User profile with ID $userId not found",
            context = context
        ) {
            override fun withContext(key: String, value: Any): ProfileNotFound {
                return copy(context = context + (key to value))
            }
        }

        data class UserOffline(
            override val context: Map<String, Any> = emptyMap()
        ) : UserError(
            code = "USER002",
            message = "User is currently offline",
            context = context
        ) {
            override fun withContext(key: String, value: Any): UserOffline {
                return copy(context = context + (key to value))
            }

            companion object {
                val INSTANCE = UserOffline()
            }
        }
    }

    /**
     * Generic Unexpected Error
     * Represents any unhandled or unexpected errors
     */
    data class UnexpectedError(
        override val message: String = "An unexpected error occurred",
        override val cause: Exception? = null,
        override val context: Map<String, Any> = emptyMap()
    ) : DomainError(
        code = "GEN001",
        message = message,
        cause = cause,
        context = context
    ) {
        override fun withContext(key: String, value: Any): UnexpectedError {
            return copy(context = context + (key to value))
        }
    }

    /**
     * Companion object for error creation and utility functions
     */
    companion object {
        /**
         * Convert a generic exception to a domain-specific error
         */
        fun fromThrowable(exception: Exception): DomainError {
            return when (exception) {
                is DomainError -> exception
                is java.net.UnknownHostException,
                is java.net.ConnectException -> NetworkError.NoConnection.INSTANCE

                is java.net.SocketTimeoutException -> NetworkError.Timeout.INSTANCE
                else -> UnexpectedError(
                    message = exception.message ?: "Unknown error",
                    cause = exception
                )
            }
        }

        /**
         * Maps HTTP status codes to appropriate domain errors
         */
        fun fromHttpStatus(statusCode: Int, message: String? = null): DomainError {
            return when (statusCode) {
                in 200..299 -> UnexpectedError("Error created from successful status code")
                401 -> AuthenticationError.Unauthorized.INSTANCE
                403 -> AuthenticationError.Forbidden.INSTANCE
                404 -> DataError.NotFound.INSTANCE
                409 -> DataError.DuplicateEntry("", message ?: "Resource conflict")
                429 -> NetworkError.ServerError(statusCode, message ?: "Too many requests")
                in 500..599 -> NetworkError.ServerError(statusCode, message ?: "Server error")
                else -> UnexpectedError(message ?: "Unknown error with status $statusCode")
            }
        }
    }
}

/**
 * Extension function to convert any exception to a domain error
 */
fun Exception.toDomainError(): DomainError =
    if (this is DomainError) this
    else DomainError.fromThrowable(this)

/**
 * Extension function for Result to map errors to domain errors
 */
fun <T> Result<T>.mapError(): Result<T> {
    return this.recoverCatching { exception ->
        // The exception in recoverCatching is a Throwable, so we need to safely cast it
        if (exception is Exception) {
            throw exception.toDomainError()
        } else {
            // If it's not an Exception (very rare case), wrap it in our UnexpectedError
            throw DomainError.UnexpectedError(
                message = exception.message ?: "Unknown error",
                cause = Exception(exception)
            )
        }
    }
}

/**
 * Extension function to create a domain success result
 */
fun <T> domainSuccess(value: T): Result<T> = Result.success(value)

/**
 * Extension function to create a domain failure result
 */
fun <T> domainFailure(error: DomainError): Result<T> = Result.failure(error)

/**
 * Extension function to run code that might throw exceptions,
 * automatically converting them to domain errors
 */
inline fun <T> runDomainCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e.toDomainError())
    }
}

/**
 * Extension function to transform Result values while preserving error handling
 */
fun <T, R> Result<T>.mapToResult(transform: (T) -> R): Result<R> {
    return if (isSuccess) {
        try {
            Result.success(transform(getOrThrow()))
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    } else {
        val exception = exceptionOrNull()
        if (exception is Exception) {
            Result.failure(exception.toDomainError())
        } else {
            // Fallback for non-Exception throwables (very rare case)
            Result.failure(
                DomainError.UnexpectedError(
                    message = exception?.message ?: "Unknown error",
                    cause = Exception(exception)
                )
            )
        }
    }
}

/**
 * Extension function to perform an action only if the result is successful,
 * preserving the original result
 */
inline fun <T> Result<T>.onDomainSuccess(action: (T) -> Unit): Result<T> {
    if (isSuccess) {
        action(getOrThrow())
    }
    return this
}

/**
 * Extension function to perform an action only if the result is a failure,
 * preserving the original result
 */
inline fun <T> Result<T>.onDomainFailure(action: (DomainError) -> Unit): Result<T> {
    val exception = exceptionOrNull()
    if (exception is Exception) {
        action(exception.toDomainError())
    } else if (exception != null) {
        // Handle the rare case of a non-Exception Throwable
        action(
            DomainError.UnexpectedError(
                message = exception.message ?: "Unknown error",
                cause = Exception(exception)
            )
        )
    }
    return this
}

/**
 * Extension function to log domain errors with additional context
 */
fun DomainError.logError(logger: (String, Exception) -> Unit) {
    logger(this.message, this)
}