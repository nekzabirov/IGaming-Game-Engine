package application.port.storage

import domain.model.Collection
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

interface ICollectionRepository {

    suspend fun save(collection: Collection): Collection

    suspend fun findByIdentity(identity: Identity): Collection?

    suspend fun findAll(pageable: Pageable): Page<Collection>

}
