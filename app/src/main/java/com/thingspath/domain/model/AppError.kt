package com.thingspath.domain.model

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    // Network
    class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause)

    // Local storage
    class StorageError(message: String, cause: Throwable? = null) : AppError(message, cause)

    // Image operations
    class ImageError(message: String, cause: Throwable? = null) : AppError(message, cause)

    // AI service
    class AiServiceError(message: String, cause: Throwable? = null) : AppError(message, cause)

    // Sync
    class SyncError(message: String, cause: Throwable? = null) : AppError(message, cause)

    // Input validation
    class ValidationError(message: String) : AppError(message)

    // Unknown
    class UnknownError(message: String, cause: Throwable? = null) : AppError(message, cause)
}
