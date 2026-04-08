package infrastructure.persistence.mapper

import domain.model.Round
import domain.vo.ExternalRoundId
import domain.vo.FreespinId
import infrastructure.persistence.entity.RoundEntity
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.mapper.SessionMapper.toDomain

object RoundMapper {

    fun RoundEntity.toDomain(): Round = Round(
        id = id.value,
        externalId = ExternalRoundId(externalId),
        freespinId = freespinId?.let { FreespinId(it) },
        session = session.toDomain(),
        gameVariant = gameVariant.toDomain(),
        createdAt = createdAt,
        finishedAt = finishedAt,
    )
}
