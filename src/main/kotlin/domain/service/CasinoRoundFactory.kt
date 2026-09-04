package domain.service

import domain.model.CasinoGame
import domain.model.CasinoRound
import domain.vo.Currency
import domain.vo.ExternalCasinoRoundId
import domain.vo.FreespinId
import domain.vo.PlayerId

object CasinoRoundFactory {

    fun open(
        playerId: PlayerId,
        game: CasinoGame?,
        currency: Currency,
        externalId: ExternalCasinoRoundId,
        freespinId: FreespinId?,
    ): CasinoRound =
        CasinoRound(
            externalId = externalId,
            freespinId = freespinId,
            playerId = playerId,
            game = game,
            currency = currency,
        )
}
