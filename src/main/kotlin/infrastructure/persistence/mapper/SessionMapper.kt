package infrastructure.persistence.mapper

import domain.model.Session
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId
import infrastructure.persistence.entity.SessionEntity
import infrastructure.persistence.mapper.GameVariantMapper.toDomain

object SessionMapper {

    fun SessionEntity.toDomain(): Session = Session(
        id = id.value,
        gameVariant = gameVariant.toDomain(),
        playerId = PlayerId(playerId),
        token = token,
        externalToken = externalToken,
        currency = Currency(currency),
        locale = Locale(locale),
        platform = platform,
    )
}
