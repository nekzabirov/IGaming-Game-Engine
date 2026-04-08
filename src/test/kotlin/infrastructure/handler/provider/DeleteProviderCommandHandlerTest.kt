package infrastructure.handler.provider

import application.command.provider.DeleteProviderCommand
import domain.exception.notfound.ProviderNotFoundException
import domain.model.Provider
import domain.repository.IProviderRepository
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DeleteProviderCommandHandlerTest : FunSpec({

    class FakeProviderRepo(private val knownIds: Set<String> = emptySet()) : IProviderRepository {
        val deleted = mutableListOf<Identity>()
        override suspend fun save(provider: Provider): Provider = provider
        override suspend fun saveAll(providers: List<Provider>): List<Provider> = providers
        override suspend fun findByIdentity(identity: Identity): Provider? = null
        override suspend fun findAll(pageable: Pageable): Page<Provider> = Page(emptyList(), 0, 0, 0)
        override suspend fun findAll(): List<Provider> = emptyList()
        override suspend fun addImage(identity: Identity, key: String, url: String) = Unit
        override suspend fun deleteByIdentity(identity: Identity) {
            if (identity.value !in knownIds) throw ProviderNotFoundException()
            deleted += identity
        }
    }

    test("delete forwards identity to repository on happy path") {
        val repo = FakeProviderRepo(knownIds = setOf("netent"))
        val handler = DeleteProviderCommandHandler(repo)

        val result = handler.handle(DeleteProviderCommand(Identity("netent")))

        result.isSuccess shouldBe true
        repo.deleted.single() shouldBe Identity("netent")
    }

    test("delete returns failure with ProviderNotFoundException when missing") {
        val repo = FakeProviderRepo(knownIds = emptySet())
        val handler = DeleteProviderCommandHandler(repo)

        val result = handler.handle(DeleteProviderCommand(Identity("nope")))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<ProviderNotFoundException>()
        repo.deleted.size shouldBe 0
    }
})
