package infrastructure.handler.collection

import application.command.collection.DeleteCollectionCommand
import domain.exception.notfound.CollectionNotFoundException
import domain.model.Collection
import domain.repository.ICollectionRepository
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DeleteCollectionCommandHandlerTest : FunSpec({

    class FakeCollectionRepo(private val knownIds: Set<String> = emptySet()) : ICollectionRepository {
        val deleted = mutableListOf<Identity>()
        override suspend fun save(collection: Collection): Collection = collection
        override suspend fun findByIdentity(identity: Identity): Collection? = null
        override suspend fun findAll(pageable: Pageable): Page<Collection> = Page(emptyList(), 0, 0, 0)
        override suspend fun addImage(identity: Identity, key: String, url: String) = Unit
        override suspend fun deleteByIdentity(identity: Identity) {
            if (identity.value !in knownIds) throw CollectionNotFoundException()
            deleted += identity
        }
    }

    test("delete forwards identity to repository on happy path") {
        val repo = FakeCollectionRepo(knownIds = setOf("popular"))
        val handler = DeleteCollectionCommandHandler(repo)

        val result = handler.handle(DeleteCollectionCommand(Identity("popular")))

        result.isSuccess shouldBe true
        repo.deleted.single() shouldBe Identity("popular")
    }

    test("delete returns failure with CollectionNotFoundException when missing") {
        val repo = FakeCollectionRepo(knownIds = emptySet())
        val handler = DeleteCollectionCommandHandler(repo)

        val result = handler.handle(DeleteCollectionCommand(Identity("ghost")))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<CollectionNotFoundException>()
        repo.deleted.size shouldBe 0
    }
})
