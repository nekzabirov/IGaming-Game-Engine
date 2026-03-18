package application.port.storage

import domain.model.Provider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

interface IProviderRepository {

    suspend fun save(provider: Provider): Provider

    suspend fun saveAll(providers: List<Provider>): List<Provider>

    suspend fun findByIdentity(identity: Identity): Provider?

    suspend fun findAll(pageable: Pageable): Page<Provider>

    suspend fun findAll(): List<Provider>

}
