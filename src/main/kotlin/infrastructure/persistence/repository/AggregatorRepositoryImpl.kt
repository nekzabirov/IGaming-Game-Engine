package infrastructure.persistence.repository

import domain.exception.domainRequireNotNull
import domain.exception.notfound.AggregatorNotFoundException
import domain.model.Aggregator
import domain.repository.IAggregatorRepository
import domain.vo.Identity
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.mapper.JsonMapperUtil.toJsonObject
import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

class AggregatorRepositoryImpl : IAggregatorRepository {

    override suspend fun save(aggregator: Aggregator): Aggregator = dbTransaction {
        AggregatorTable.upsert(keys = arrayOf(AggregatorTable.identity)) {
            it[identity] = aggregator.identity.value
            it[integration] = aggregator.integration
            it[config] = aggregator.config.toJsonObject()
            it[active] = aggregator.active
        }

        aggregator
    }

    override suspend fun findByIdentity(identity: Identity): Aggregator? = dbRead {
        AggregatorTable
            .selectAll()
            .where { AggregatorTable.identity eq identity.value }
            .singleOrNull()
            ?.toAggregator()
    }

    override suspend fun findAllByIdentities(identities: List<Identity>): List<Aggregator> = dbRead {
        if (identities.isEmpty()) return@dbRead emptyList()
        AggregatorTable
            .selectAll()
            .where { AggregatorTable.identity inList identities.map { it.value } }
            .map { it.toAggregator() }
    }

    override suspend fun findAll(): List<Aggregator> = dbRead {
        AggregatorTable
            .selectAll()
            .map { it.toAggregator() }
    }

    override suspend fun deleteByIdentity(identity: Identity) {
        dbTransaction {
            val existed = AggregatorTable
                .selectAll()
                .where { AggregatorTable.identity eq identity.value }
                .any()
            domainRequireNotNull(if (existed) Unit else null) { AggregatorNotFoundException() }

            val identityValue = identity.value
            AggregatorTable.deleteWhere { AggregatorTable.identity eq identityValue }
        }
    }
}
