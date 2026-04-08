package domain.model

import domain.service.RoundFactory
import domain.vo.Currency
import domain.vo.ExternalRoundId
import domain.vo.FreespinId
import domain.vo.Locale
import domain.vo.PlayerId
import domain.vo.SessionToken

data class Session(
    val id: Long = Long.MIN_VALUE,

    val gameVariant: GameVariant,

    val playerId: PlayerId,

    val token: SessionToken,

    val externalToken: String?,

    val currency: Currency,

    val locale: Locale,

    val platform: Platform,
) {
    /**
     * Opens a new [Round] against this session. Keeps round creation anchored to its
     * parent aggregate so usecases don't have to know about [RoundFactory].
     */
    fun openRound(externalId: ExternalRoundId, freespinId: FreespinId? = null): Round =
        RoundFactory.open(session = this, externalId = externalId, freespinId = freespinId)
}
