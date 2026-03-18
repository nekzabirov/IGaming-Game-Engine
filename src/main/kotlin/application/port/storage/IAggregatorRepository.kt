package application.port.storage

import domain.model.Aggregator
import domain.vo.Identity

interface IAggregatorRepository {

    suspend fun save(aggregator: Aggregator): Aggregator

    suspend fun findByIdentity(identity: Identity): Aggregator?

    suspend fun findAll(): List<Aggregator>

}
