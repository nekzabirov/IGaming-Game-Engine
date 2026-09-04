package domain.repository

import domain.model.CasinoGame
import domain.model.CasinoRound
import domain.vo.Currency
import domain.vo.ExternalCasinoRoundId
import domain.vo.FreespinId
import domain.vo.PlayerId

interface ICasinoRoundRepository {

    suspend fun save(round: CasinoRound): CasinoRound

    suspend fun findById(id: Long): CasinoRound?

    suspend fun findByExternalId(externalId: ExternalCasinoRoundId): CasinoRound?

    /**
     * Finds the round the hub already knows by [externalId], or opens it. The race between two
     * legs of the same brand-new round arriving at once is decided by the table's unique index on
     * `external_id`, not by a check-then-insert in this method — the second caller reads back the
     * row the first one just created.
     */
    suspend fun findOrCreate(
        externalId: ExternalCasinoRoundId,
        playerId: PlayerId,
        game: CasinoGame?,
        currency: Currency,
        freespinId: FreespinId?,
    ): CasinoRound
}
