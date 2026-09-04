package infrastructure.persistence.mapper

import domain.model.CasinoRound
import domain.vo.Currency
import domain.vo.ExternalCasinoRoundId
import domain.vo.FreespinId
import domain.vo.PlayerId
import infrastructure.persistence.entity.CasinoRoundEntity
import infrastructure.persistence.mapper.CasinoGameMapper.toDomain

object CasinoRoundMapper {

    fun CasinoRoundEntity.toDomain(): CasinoRound = CasinoRound(
        id = id.value,
        externalId = ExternalCasinoRoundId(externalId),
        freespinId = freespinId?.let { FreespinId(it) },
        playerId = PlayerId(playerId),
        game = game?.toDomain(),
        currency = Currency(currency),
        createdAt = createdAt,
        finishedAt = finishedAt,
    )
}
