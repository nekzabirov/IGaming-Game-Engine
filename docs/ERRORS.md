# Error handling

Domain exceptions (`errors/Errors.kt`) are mapped by category to a gRPC status by
`grpc/Grpc.kt#handleGrpcCall`; the class simple name travels in the `x-exception-name` trailer.

| Category | gRPC status | Exceptions |
| --- | --- | --- |
| `NotFoundException` | `NOT_FOUND` | `CasinoGameNotFoundException`, `CasinoGameNotRoutedException`, `CasinoProviderNotFoundException`, `CasinoRoundNotFoundException`, `CollectionNotFoundException` |
| `BadRequestException` | `INVALID_ARGUMENT` | `Blank*Exception`, `EmptyIdentityException`, `InvalidIdentityFormatException`, `InvalidAmountException`, `InvalidDateFormatException`, `UnspecifiedRtpTypeException`, `UnsupportedPlatformException` |
| `ConflictException` | `ALREADY_EXISTS` | `CasinoGameNotActiveException`, `CasinoGameUnavailableException`, `CasinoProviderNotActiveException`, `CasinoRoundAlreadyFinishedException`, `FreespinNotSupportedException`, `SpinAlreadyExistsException` |
| `ForbiddenException` | `PERMISSION_DENIED` | `InsufficientBalanceException`, `MaxPlaceSpinException` |
| `SystemException` | `INTERNAL` | `GameHubUnavailableException` |

Anything else is `INTERNAL` with a generic description.

## The hub's wallet calls

`gamehub.v1.WebhookService` answers with a machine-readable `x-error-code` trailer instead:
`INSUFFICIENT_FUNDS` and `LIMIT_EXCEEDED` (`FAILED_PRECONDITION`), `PLAYER_NOT_FOUND`
(`UNAUTHENTICATED` for bad credentials, `NOT_FOUND` for an unknown round), `INTERNAL` for an
unknown outcome — the one code that starts a vendor rollback cycle, never used for a refusal.

## Client-side handling

```kotlin
try {
    gameClient.play(command)
} catch (e: StatusRuntimeException) {
    when (e.trailers?.get(Metadata.Key.of("x-exception-name", Metadata.ASCII_STRING_MARSHALLER))) {
        "CasinoGameUnavailableException" -> showGameUnavailable()
        "CasinoGameNotActiveException" -> showAlternativeGames()
        else -> showGenericError()
    }
}
```
