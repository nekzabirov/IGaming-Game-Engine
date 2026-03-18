package application.port.storage

import domain.model.GameVariant
import domain.vo.Identity

interface IGameVariantRepository {

    suspend fun save(gameVariant: GameVariant): GameVariant

    suspend fun saveAll(gameVariants: List<GameVariant>): List<GameVariant>

    suspend fun findById(id: Long): GameVariant?

    suspend fun findBySymbol(symbol: String): GameVariant?

    suspend fun findAllByGame(gameIdentity: Identity): List<GameVariant>

    suspend fun findActiveByGameIdentity(gameIdentity: Identity): GameVariant?

    suspend fun findAllByIntegration(integration: String): List<GameVariant>

}
