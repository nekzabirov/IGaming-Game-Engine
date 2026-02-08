package infrastructure.api.grpc.error

import shared.Logger

/**
 * Extension functions for convenient error handling in gRPC services.
 */

/**
 * Maps a Result to throw StatusException on failure.
 * Converts DomainError to structured gRPC error with metadata.
 */
fun <T> Result<T>.getOrThrowGrpc(): T {
    return getOrElse { error ->
        Logger.error("[GRPC] Result failure: ${error.message}")
        throw GrpcErrorMapper.toStatusException(error)
    }
}

/**
 * Maps a Result, applying a transform on success or throwing StatusException on failure.
 */
inline fun <T, R> Result<T>.mapOrThrowGrpc(transform: (T) -> R): R {
    return map(transform).getOrThrowGrpc()
}
