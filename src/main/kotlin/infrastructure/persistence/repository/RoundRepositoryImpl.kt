package infrastructure.persistence.repository

import application.port.storage.IRoundRepository
import domain.model.Round
import infrastructure.persistence.entity.GameEntity
import infrastructure.persistence.entity.GameVariantEntity
import infrastructure.persistence.entity.ProviderEntity
import infrastructure.persistence.entity.RoundEntity
import infrastructure.persistence.entity.SessionEntity
import infrastructure.persistence.mapper.RoundMapper.toDomain
import infrastructure.persistence.table.RoundTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class RoundRepositoryImpl : IRoundRepository {

    private val roundChain = arrayOf(
        RoundEntity::session,
        RoundEntity::gameVariant,
        SessionEntity::gameVariant,
        GameVariantEntity::game,
        GameEntity::provider,
        GameEntity::collections,
        ProviderEntity::aggregator,
    )

    override suspend fun save(round: Round): Round = newSuspendedTransaction {
        if (round.id == Long.MIN_VALUE) {
            val id = RoundTable.insertAndGetId { it.fromDomain(round) }
            round.copy(id = id.value)
        } else {
            RoundTable.update({ RoundTable.id eq round.id }) { it.fromDomain(round) }
            round
        }
    }

    override suspend fun findById(id: Long): Round? = newSuspendedTransaction {
        RoundEntity.findById(id)
            ?.load(*roundChain)
            ?.toDomain()
    }

    override suspend fun findByExternalIdAndSessionId(externalId: String, sessionId: Long): Round? = newSuspendedTransaction {
        RoundEntity.find { (RoundTable.externalId eq externalId) and (RoundTable.session eq sessionId) }
            .with(*roundChain)
            .firstOrNull()?.toDomain()
    }

    private fun UpdateBuilder<*>.fromDomain(round: Round) {
        this[RoundTable.externalId] = round.externalId
        this[RoundTable.freespinId] = round.freespinId
        this[RoundTable.session] = round.session.id
        this[RoundTable.gameVariant] = round.gameVariant.id
        this[RoundTable.createdAt] = round.createdAt
        this[RoundTable.finishedAt] = round.finishedAt
    }
}
