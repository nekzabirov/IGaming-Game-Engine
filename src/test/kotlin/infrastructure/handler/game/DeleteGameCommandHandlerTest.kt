package infrastructure.handler.game

import application.command.game.DeleteGameCommand
import domain.exception.notfound.GameNotFoundException
import domain.model.Game
import domain.repository.IGameRepository
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DeleteGameCommandHandlerTest : FunSpec({

    class FakeGameRepo(private val knownIds: Set<String> = emptySet()) : IGameRepository {
        val deleted = mutableListOf<Identity>()
        override suspend fun save(game: Game): Game = game
        override suspend fun saveAll(gameList: List<Game>): List<Game> = gameList
        override suspend fun findByIdentity(identity: Identity): Game? = null
        override suspend fun findAll(pageable: Pageable): Page<Game> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<Game> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) = Unit
        override suspend fun deleteByIdentity(identity: Identity) {
            if (identity.value !in knownIds) throw GameNotFoundException()
            deleted += identity
        }
    }

    test("delete forwards identity to repository on happy path") {
        val repo = FakeGameRepo(knownIds = setOf("game_a"))
        val handler = DeleteGameCommandHandler(repo)

        val result = handler.handle(DeleteGameCommand(Identity("game_a")))

        result.isSuccess shouldBe true
        repo.deleted.single() shouldBe Identity("game_a")
    }

    test("delete returns failure with GameNotFoundException when missing") {
        val repo = FakeGameRepo(knownIds = emptySet())
        val handler = DeleteGameCommandHandler(repo)

        val result = handler.handle(DeleteGameCommand(Identity("game_missing")))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<GameNotFoundException>()
        repo.deleted.size shouldBe 0
    }
})
