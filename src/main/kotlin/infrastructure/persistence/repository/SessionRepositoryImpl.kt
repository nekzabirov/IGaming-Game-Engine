package infrastructure.persistence.repository

import domain.repository.ISessionRepository
import domain.model.Session
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.entity.SessionEntity
import infrastructure.persistence.mapper.SessionMapper.toDomain
import infrastructure.persistence.table.SessionTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update

class SessionRepositoryImpl : ISessionRepository {

    override suspend fun save(session: Session): Session = dbTransaction {
        if (session.id == Long.MIN_VALUE) {
            val id = SessionTable.insertAndGetId { it.fromDomain(session) }
            session.copy(id = id.value)
        } else {
            SessionTable.update({ SessionTable.id eq session.id }) { it.fromDomain(session) }
            session
        }
    }

    override suspend fun findById(id: Long): Session? = dbRead {
        SessionEntity.findById(id)
            ?.load(
                SessionEntity::gameVariant,
                GameVariantEntity::game,
                GameEntity::provider,
                GameEntity::collections,
                ProviderEntity::aggregator,
            )
            ?.toDomain()
    }

    override suspend fun findByToken(token: String): Session? = dbRead {
        SessionEntity.find { SessionTable.token eq token }
            .with(
                SessionEntity::gameVariant,
                GameVariantEntity::game,
                GameEntity::provider,
                GameEntity::collections,
                ProviderEntity::aggregator,
            )
            .firstOrNull()?.toDomain()
    }

    private fun UpdateBuilder<*>.fromDomain(session: Session) {
        this[SessionTable.gameVariant] = session.gameVariant.id
        this[SessionTable.playerId] = session.playerId.value
        this[SessionTable.token] = session.token.value
        this[SessionTable.externalToken] = session.externalToken
        this[SessionTable.currency] = session.currency.value
        this[SessionTable.locale] = session.locale.value
        this[SessionTable.platform] = session.platform
    }
}
