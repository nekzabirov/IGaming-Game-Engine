package api.grpc.config

import domain.exception.DomainException
import domain.exception.badrequest.BadRequestException
import domain.exception.conflict.ConflictException
import domain.exception.forbidden.ForbiddenException
import domain.exception.notfound.NotFoundException
import domain.exception.system.SystemException
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException

val EXCEPTION_NAME_KEY: Metadata.Key<String> =
    Metadata.Key.of("x-exception-name", Metadata.ASCII_STRING_MARSHALLER)

suspend fun <T> handleGrpcCall(block: suspend () -> T): T {
    try {
        return block()
    } catch (e: StatusException) {
        throw e
    } catch (e: DomainException) {
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
        throw StatusException(Status.INTERNAL.withDescription("Internal server error"))
    }
}
