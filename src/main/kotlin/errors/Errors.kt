package errors

import db.Platform

// The class simple name travels to gRPC callers in the `x-exception-name` trailer (casino-app
// switches on `CasinoGameUnavailableException`), and the category decides the status code — so
// names and categories here are part of the wire contract.

abstract class DomainException(message: String) : RuntimeException(message)

// ------------------------------------------------------------------------------- NOT_FOUND --

sealed class NotFoundException(message: String) : DomainException(message)

class CasinoGameNotFoundException : NotFoundException("CasinoGame not found")

/** The game exists, but no partner is routed for it at this operator — the hub's `NO_ROUTE`. */
class CasinoGameNotRoutedException : NotFoundException("CasinoGame is not routed for this operator")

class CasinoProviderNotFoundException : NotFoundException("CasinoProvider not found")

class CasinoRoundNotFoundException : NotFoundException("CasinoRound not found")

class CollectionNotFoundException : NotFoundException("Collection not found")

// -------------------------------------------------------------------------- INVALID_ARGUMENT --

sealed class BadRequestException(message: String) : DomainException(message)

class BlankCountryException : BadRequestException("Country cannot be blank")

class BlankCurrencyException : BadRequestException("Currency code cannot be blank")

class BlankExternalIdException : BadRequestException("External id cannot be blank")

class BlankFreespinIdException : BadRequestException("Freespin id cannot be blank")

class BlankImageUrlException : BadRequestException("Image URL cannot be blank")

class BlankLocaleException : BadRequestException("Locale cannot be blank")

class BlankPlayerIdException : BadRequestException("Player ID cannot be blank")

class EmptyIdentityException : BadRequestException("Identity value must not be empty")

class InvalidAmountException(value: Long) : BadRequestException("Amount must be non-negative, got: $value")

class InvalidDateFormatException(field: String, value: String) : BadRequestException(
    "Field `$field` must be a full ISO-8601 instant (e.g. 2026-09-01T00:00:00Z), got: `$value`",
)

class InvalidIdentityFormatException : BadRequestException(
    "Identity must contain only lowercase letters, digits, and '_'. Use '_' instead of spaces.",
)

class UnspecifiedRtpTypeException : BadRequestException("RTP type must be HOT or COLD")

class UnsupportedPlatformException(platform: Platform) : BadRequestException("Platform $platform is not supported")

// --------------------------------------------------------------------------- ALREADY_EXISTS --

sealed class ConflictException(message: String) : DomainException(message)

class CasinoGameNotActiveException : ConflictException("CasinoGame should be active")

/**
 * The vendor answered and refused to open the game — the hub's `VENDOR_REFUSED`: a currency not
 * enabled on the account, a title closed for the player's country. A refusal, not an outage.
 */
class CasinoGameUnavailableException : ConflictException("CasinoGame is unavailable at the vendor")

class CasinoProviderNotActiveException : ConflictException("CasinoProvider should be active")

class CasinoRoundAlreadyFinishedException : ConflictException("CasinoRound is already finished")

class FreespinNotSupportedException : ConflictException("CasinoGame variant does not support freespins")

/** Raised by the unique constraint on `spins.external_id` — a redelivered leg is already committed. */
class SpinAlreadyExistsException : ConflictException("Spin already exists")

// ------------------------------------------------------------------------ PERMISSION_DENIED --

sealed class ForbiddenException(message: String) : DomainException(message)

class InsufficientBalanceException : ForbiddenException("Insufficient balance")

class MaxPlaceSpinException : ForbiddenException("Max place spin is required")

// --------------------------------------------------------------------------------- INTERNAL --

sealed class SystemException(message: String) : DomainException(message)

class GameHubUnavailableException(message: String? = null) : SystemException(message ?: "GameHub is unavailable")

class WalletUnavailableException(message: String? = null) : SystemException(message ?: "Wallet is unavailable")

// ------------------------------------------------------------------------------ validation --

/** Input checks at the API boundary. Each returns the value it accepted, so it reads inline. */
object Valid {

    /** Fleet-wide canonical slug: lowercase alphanumerics joined by single `-` or `_`. */
    private val SLUG = Regex("^[a-z0-9]+(?:[-_][a-z0-9]+)*$")

    fun identity(value: String): String {
        if (value.isEmpty()) throw EmptyIdentityException()
        if (!value.matches(SLUG)) throw InvalidIdentityFormatException()
        return value
    }

    fun playerId(value: String): String = value.ifBlank { throw BlankPlayerIdException() }

    fun currency(value: String): String = value.ifBlank { throw BlankCurrencyException() }

    fun locale(value: String): String = value.ifBlank { throw BlankLocaleException() }

    fun country(value: String): String = value.ifBlank { throw BlankCountryException() }

    fun externalId(value: String): String = value.ifBlank { throw BlankExternalIdException() }

    fun freespinId(value: String): String = value.ifBlank { throw BlankFreespinIdException() }

    fun imageUrl(value: String): String = value.ifBlank { throw BlankImageUrlException() }

    /** Wallet system unit (nano = value x 1e9); direction is carried by the call, never by the sign. */
    fun amount(value: Long): Long {
        if (value < 0) throw InvalidAmountException(value)
        return value
    }
}
