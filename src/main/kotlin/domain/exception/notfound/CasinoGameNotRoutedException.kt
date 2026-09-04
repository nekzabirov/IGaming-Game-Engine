package domain.exception.notfound

/** The game exists, but no partner is routed for it at this operator — the hub's `NO_ROUTE`. */
class CasinoGameNotRoutedException : NotFoundException("CasinoGame is not routed for this operator")
