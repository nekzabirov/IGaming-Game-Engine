package grpc

import errors.BadRequestException
import errors.ConflictException
import errors.DomainException
import errors.ForbiddenException
import errors.NotFoundException
import errors.SystemException
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("grpc")

/** The exception's simple name, for callers that switch on it (casino-app does). */
val EXCEPTION_NAME_KEY: Metadata.Key<String> = Metadata.Key.of("x-exception-name", Metadata.ASCII_STRING_MARSHALLER)

/** Wraps every RPC of the operator-facing services: category -> status, name -> trailer. */
suspend fun <T> handleGrpcCall(block: suspend () -> T): T {
    try {
        return block()
    } catch (e: StatusException) {
        throw e
    } catch (e: DomainException) {
        log.info("Domain rejection [{}]: {}", e::class.simpleName, e.message)
        val status = when (e) {
            is NotFoundException -> Status.NOT_FOUND
            is BadRequestException -> Status.INVALID_ARGUMENT
            is ConflictException -> Status.ALREADY_EXISTS
            is ForbiddenException -> Status.PERMISSION_DENIED
            is SystemException -> Status.INTERNAL
            else -> Status.INTERNAL
        }
        val metadata = Metadata()
        metadata.put(EXCEPTION_NAME_KEY, e::class.simpleName ?: "Unknown")
        throw StatusException(status.withDescription(e.message), metadata)
    } catch (e: Exception) {
        log.error("Unhandled exception in gRPC call", e)
        throw StatusException(Status.INTERNAL.withDescription("Internal server error"))
    }
}
