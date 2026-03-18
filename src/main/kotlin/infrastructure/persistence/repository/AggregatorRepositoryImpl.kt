package infrastructure.persistence.repository

import application.port.storage.IAggregatorRepository
import domain.model.Aggregator
import domain.vo.Identity
import infrastructure.persistence.mapper.AggregatorMapper.toAggregator
import infrastructure.persistence.mapper.JsonMapperUtil.toJsonObject
import infrastructure.persistence.table.AggregatorTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

class AggregatorRepositoryImpl : IAggregatorRepository {

    override suspend fun save(aggregator: Aggregator): Aggregator = newSuspendedTransaction {
        AggregatorTable.upsert(keys = arrayOf(AggregatorTable.identity)) {
            it[identity] = aggregator.identity.value
            it[integration] = aggregator.integration
            it[config] = aggregator.config.toJsonObject()
            it[active] = aggregator.active
        }

        aggregator
    }

    override suspend fun findByIdentity(identity: Identity): Aggregator? = newSuspendedTransaction {
        AggregatorTable
            .selectAll()
            .where { AggregatorTable.identity eq identity.value }
            .singleOrNull()
            ?.toAggregator()
    }

    override suspend fun findAll(): List<Aggregator> = newSuspendedTransaction {
        AggregatorTable
            .selectAll()
            .map { it.toAggregator() }
    }
}
