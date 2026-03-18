package application.port.storage

import domain.model.Spin

interface ISpinRepository {

    suspend fun save(spin: Spin): Spin

    suspend fun findById(id: Long): Spin?

    suspend fun findByExternalId(externalId: String): Spin?

}
