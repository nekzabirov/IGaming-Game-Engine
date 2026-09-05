package domain.repository

import domain.model.CasinoGame
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

interface ICasinoGameRepository {

    suspend fun save(game: CasinoGame): CasinoGame

    suspend fun saveAll(gameList: List<CasinoGame>): List<CasinoGame>

    suspend fun findByIdentity(identity: Identity): CasinoGame?

    suspend fun findAll(pageable: Pageable): Page<CasinoGame>

    suspend fun findAll(): List<CasinoGame>

    suspend fun addImage(identity: Identity, key: String, url: String)

    /** Replaces the LOCAL tag list. `tags` stays untouched — it belongs to the catalog sync, which
     *  overwrites it wholesale on every run. */
    suspend fun setCustomTags(identity: Identity, tags: List<String>)

}
