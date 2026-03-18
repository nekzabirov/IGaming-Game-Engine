package infrastructure.persistence.mapper

import domain.model.Round
import infrastructure.persistence.entity.RoundEntity
import infrastructure.persistence.mapper.GameVariantMapper.toDomain
import infrastructure.persistence.mapper.SessionMapper.toDomain

object RoundMapper {

    fun RoundEntity.toDomain(): Round = Round(
        id = id.value,
        externalId = externalId,
        freespinId = freespinId,
        session = session.toDomain(),
        gameVariant = gameVariant.toDomain(),
        createdAt = createdAt,
        finishedAt = finishedAt,
    )
}
