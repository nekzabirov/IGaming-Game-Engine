package domain.model

import domain.exception.badrequest.BlankSessionTokenException
import domain.exception.domainRequire
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId

data class Session(
    val id: Long = Long.MIN_VALUE,

    val gameVariant: GameVariant,

    val playerId: PlayerId,

    val token: String,

    val externalToken: String?,

    val currency: Currency,

    val locale: Locale,

    val platform: Platform
) {
    init {
        domainRequire(token.isNotBlank()) { BlankSessionTokenException() }
    }
}
