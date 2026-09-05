package domain.repository

import domain.model.CasinoProvider
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

interface ICasinoProviderRepository {

    suspend fun save(provider: CasinoProvider): CasinoProvider

    suspend fun saveAll(providers: List<CasinoProvider>): List<CasinoProvider>

    suspend fun findByIdentity(identity: Identity): CasinoProvider?

    suspend fun findAll(pageable: Pageable): Page<CasinoProvider>

    suspend fun findAll(): List<CasinoProvider>

    suspend fun addImage(identity: Identity, key: String, url: String)

    /** Replaces the LOCAL tag list. `tags` stays untouched — it belongs to the catalog sync, which
     *  overwrites it wholesale on every run. */
    suspend fun setCustomTags(identity: Identity, tags: List<String>)

}
