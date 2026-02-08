package infrastructure.persistence.exposed.repository

import application.port.outbound.storage.AggregatorInfoRepository
import domain.aggregator.AggregatorInfo
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of AggregatorInfoRepository.
 */
class ExposedAggregatorInfoRepository : AggregatorInfoRepository {

    override suspend fun findById(id: UUID): AggregatorInfo? =
        newSuspendedTransaction {
            AggregatorInfoTable.selectAll()
                .where { AggregatorInfoTable.id eq id }
                .singleOrNull()
                ?.toAggregatorInfo()
        }

    override suspend fun findByIdentity(identity: String): AggregatorInfo? =
        newSuspendedTransaction {
            AggregatorInfoTable.selectAll()
                .where { AggregatorInfoTable.identity eq identity }
                .singleOrNull()
                ?.toAggregatorInfo()
        }
}
