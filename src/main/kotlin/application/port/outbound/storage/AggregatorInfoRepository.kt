package application.port.outbound.storage

import domain.aggregator.AggregatorInfo
import java.util.UUID

/**
 * Port interface for AggregatorInfo entity persistence operations.
 */
interface AggregatorInfoRepository {
    /**
     * Find aggregator info by ID.
     */
    suspend fun findById(id: UUID): AggregatorInfo?

    /**
     * Find aggregator info by identity.
     */
    suspend fun findByIdentity(identity: String): AggregatorInfo?
}
