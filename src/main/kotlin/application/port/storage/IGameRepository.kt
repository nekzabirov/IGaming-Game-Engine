package application.port.storage

import domain.model.Game
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

interface IGameRepository {

    suspend fun save(game: Game): Game

    suspend fun saveAll(gameList: List<Game>): List<Game>

    suspend fun findByIdentity(identity: Identity): Game?

    suspend fun findAll(pageable: Pageable): Page<Game>

    suspend fun findAll(): List<Game>

}
