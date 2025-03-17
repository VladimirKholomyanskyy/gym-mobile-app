package com.neyra.gymapp.domain.error

/**
 * Sealed class representing domain-specific errors
 */
sealed class DomainError(
    override val message: String,
    override val cause: Throwable? = null
) : Throwable(message, cause) {

    // Authentication and Authorization Errors
    sealed class AuthenticationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause) {
        data object Unauthorized : AuthenticationError("User is not authenticated") {
            private fun readResolve(): Any = Unauthorized
        }

        data object Forbidden : AuthenticationError("User does not have permission") {
            private fun readResolve(): Any = Forbidden
        }

        data class TokenExpired(
            override val message: String = "Authentication token has expired"
        ) : AuthenticationError(message)
    }

    // Network-related Errors
    sealed class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause) {
        data object NoConnection : NetworkError("No internet connection") {
            private fun readResolve(): Any = NoConnection
        }

        data object Timeout : NetworkError("Network request timed out") {
            private fun readResolve(): Any = Timeout
        }

        data class ServerError(
            val code: Int,
            override val message: String = "Server error occurred"
        ) : NetworkError(message)
    }

    // Validation Errors
    sealed class ValidationError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause) {
        data class InvalidName(
            val name: String,
            override val message: String = "Invalid training program name"
        ) : ValidationError(message)

        data class InvalidDescription(
            val description: String,
            override val message: String = "Invalid description"
        ) : ValidationError(message)

        data object WorkoutLimitExceeded : ValidationError("Maximum number of workouts exceeded") {
            private fun readResolve(): Any = WorkoutLimitExceeded
        }
    }

    // Data-related Errors
    sealed class DataError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause) {
        data object NotFound : DataError("Requested resource not found") {
            private fun readResolve(): Any = NotFound
        }

        data class DuplicateEntry(
            val identifier: String,
            override val message: String = "Duplicate entry exists"
        ) : DataError(message)

        data object DatabaseError : DataError("Database operation failed") {
            private fun readResolve(): Any = DatabaseError
        }
    }

    // Synchronization Errors
    sealed class SyncError(
        override val message: String,
        override val cause: Throwable? = null
    ) : DomainError(message, cause) {
        data object PartialSync : SyncError("Some items were not synchronized") {
            private fun readResolve(): Any = PartialSync
        }

        data object SyncFailed : SyncError("Synchronization failed") {
            private fun readResolve(): Any = SyncFailed
        }
    }

    // Generic Unexpected Error
    data class UnexpectedError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : DomainError(message, cause)

    // Companion object for error creation helpers
    companion object {
        /**
         * Convert a generic exception to a domain-specific error
         */
        fun fromThrowable(throwable: Throwable): DomainError {
            return when (throwable) {
                is DomainError -> throwable
                is java.net.UnknownHostException,
                is java.net.ConnectException -> NetworkError.NoConnection

                is java.net.SocketTimeoutException -> NetworkError.Timeout
                else -> UnexpectedError(
                    message = throwable.message ?: "Unknown error",
                    cause = throwable
                )
            }
        }
    }
}

/**
 * Extension function to handle domain errors
 */
fun Throwable.toDomainError(): DomainError =
    if (this is DomainError) this
    else DomainError.fromThrowable(this)

/**
 * Extension function for Result to map errors
 */
fun <T> Result<T>.mapError(): Result<T> {
    return this.recoverCatching { throwable ->
        throw throwable.toDomainError()
    }
}