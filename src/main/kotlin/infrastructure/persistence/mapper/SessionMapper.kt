package infrastructure.persistence.mapper

import domain.model.Session
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId
import domain.vo.SessionToken
import infrastructure.persistence.entity.SessionEntity
import infrastructure.persistence.mapper.GameVariantMapper.toDomain

object SessionMapper {

    fun SessionEntity.toDomain(): Session = Session(
        id = id.value,
        gameVariant = gameVariant.toDomain(),
        playerId = PlayerId(playerId),
        token = SessionToken(token),
        externalToken = externalToken,
        currency = Currency(currency),
        locale = Locale(locale),
        platform = platform,
    )
}
