package domain.repository

import domain.model.Collection
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

interface ICollectionRepository {

    suspend fun save(collection: Collection): Collection

    suspend fun findByIdentity(identity: Identity): Collection?

    suspend fun findAll(pageable: Pageable): Page<Collection>

    suspend fun addImage(identity: Identity, key: String, url: String)

    /**
     * Add a single game to a collection. Idempotent: if [gameIdentity] is
     * already in [identity], no-op. On first insert, sort order is set to
     * `max(existing sort_order) + 1` (or `0` when the collection is empty).
     *
     * Raises `CollectionNotFoundException` if [identity] does not exist, or
     * `GameNotFoundException` if [gameIdentity] does not exist.
     */
    suspend fun addGame(identity: Identity, gameIdentity: Identity)

    /**
     * Remove a single game from a collection. Idempotent: if [gameIdentity]
     * is not currently a member, no-op.
     *
     * Raises `CollectionNotFoundException` if [identity] does not exist, or
     * `GameNotFoundException` if [gameIdentity] does not exist.
     */
    suspend fun removeGame(identity: Identity, gameIdentity: Identity)

    /**
     * Set the per-collection [order] of [gameIdentity] inside [identity].
     *
     * Raises `CollectionNotFoundException` if [identity] does not exist, or
     * `GameNotFoundException` if the (collection, game) row does not exist.
     */
    suspend fun updateGameOrder(identity: Identity, gameIdentity: Identity, order: Int)

}
