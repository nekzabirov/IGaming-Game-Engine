package application.port.storage

import domain.model.Session

interface ISessionRepository {

    suspend fun save(session: Session): Session

    suspend fun findById(id: Long): Session?

    suspend fun findByToken(token: String): Session?

}
