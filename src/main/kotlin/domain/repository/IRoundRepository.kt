package domain.repository

import domain.model.Round
import domain.vo.ExternalRoundId

interface IRoundRepository {

    suspend fun save(round: Round): Round

    suspend fun findById(id: Long): Round?

    suspend fun findByExternalIdAndSessionId(externalId: ExternalRoundId, sessionId: Long): Round?
}
