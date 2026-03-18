package application.port.storage

import domain.model.Round

interface IRoundRepository {

    suspend fun save(round: Round): Round

    suspend fun findById(id: Long): Round?

    suspend fun findByExternalIdAndSessionId(externalId: String, sessionId: Long): Round?

}
