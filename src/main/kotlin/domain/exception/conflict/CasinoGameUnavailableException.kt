package domain.exception.conflict

/**
 * The vendor answered and refused to open the game — the hub's `VENDOR_REFUSED`: a currency not
 * enabled on the account, a title closed for the player's country, a grant the vendor would not
 * take. A refusal, not an outage: retrying yields the same answer, and the player should see
 * "unavailable", not a server error.
 */
class CasinoGameUnavailableException : ConflictException("CasinoGame is unavailable at the vendor")
