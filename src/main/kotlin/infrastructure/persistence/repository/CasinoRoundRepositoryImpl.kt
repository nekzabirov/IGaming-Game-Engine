package infrastructure.persistence.repository

import domain.exception.domainRequireNotNull
import domain.exception.notfound.CasinoGameNotFoundException
import domain.model.CasinoGame
import domain.model.CasinoRound
import domain.repository.ICasinoRoundRepository
import domain.vo.Currency
import domain.vo.ExternalCasinoRoundId
import domain.vo.FreespinId
import domain.vo.PlayerId
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.CasinoGameEntity
import infrastructure.persistence.entity.CasinoRoundEntity
import infrastructure.persistence.mapper.CasinoRoundMapper.toDomain
import infrastructure.persistence.table.CasinoGameTable
import infrastructure.persistence.table.CasinoRoundTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert

class CasinoRoundRepositoryImpl : ICasinoRoundRepository {

    private val roundChain = arrayOf(
        CasinoRoundEntity::game,
        CasinoGameEntity::provider,
        CasinoGameEntity::collections,
    )

    override suspend fun save(round: CasinoRound): CasinoRound = dbTransaction {
        if (round.id == Long.MIN_VALUE) {
            val id = CasinoRoundTable.insertAndGetId { it.fromDomain(round) }
            round.copy(id = id.value)
        } else {
            CasinoRoundTable.update({ CasinoRoundTable.id eq round.id }) { it.fromDomain(round) }
            round
        }
    }

    override suspend fun findById(id: Long): CasinoRound? = dbRead {
        CasinoRoundEntity.findById(id)
            ?.load(*roundChain)
            ?.toDomain()
    }

    override suspend fun findByExternalId(externalId: ExternalCasinoRoundId): CasinoRound? = dbRead {
        CasinoRoundEntity.find { CasinoRoundTable.externalId eq externalId.value }
            .with(*roundChain)
            .firstOrNull()?.toDomain()
    }

    override suspend fun findOrCreate(
        externalId: ExternalCasinoRoundId,
        playerId: PlayerId,
        game: CasinoGame?,
        currency: Currency,
        freespinId: FreespinId?,
    ): CasinoRound = dbTransaction {
        val gameEntityId = game?.let {
            domainRequireNotNull(
                CasinoGameEntity.find { CasinoGameTable.identity eq it.identity.value }.firstOrNull()
            ) { CasinoGameNotFoundException() }.id
        }

        // ON CONFLICT ... DO UPDATE SET external_id = EXCLUDED.external_id RETURNING * — the loser
        // of the race reads back the row the winner just inserted, rather than the two callers
        // checking-then-inserting against each other.
        val row = CasinoRoundTable.upsert(
            CasinoRoundTable.externalId,
            onUpdate = { it[CasinoRoundTable.externalId] = CasinoRoundTable.externalId },
        ) {
            it[CasinoRoundTable.externalId] = externalId.value
            it[CasinoRoundTable.freespinId] = freespinId?.value
            it[CasinoRoundTable.playerId] = playerId.value
            it[CasinoRoundTable.game] = gameEntityId
            it[CasinoRoundTable.currency] = currency.value
        }.resultedValues!!.single()

        CasinoRoundEntity.wrapRow(row).load(*roundChain).toDomain()
    }

    private fun UpdateBuilder<*>.fromDomain(round: CasinoRound) {
        this[CasinoRoundTable.externalId] = round.externalId.value
        this[CasinoRoundTable.freespinId] = round.freespinId?.value
        this[CasinoRoundTable.playerId] = round.playerId.value
        this[CasinoRoundTable.game] = round.game?.let { game ->
            CasinoGameEntity.find { CasinoGameTable.identity eq game.identity.value }.firstOrNull()?.id
        }
        this[CasinoRoundTable.currency] = round.currency.value
        this[CasinoRoundTable.createdAt] = round.createdAt
        this[CasinoRoundTable.finishedAt] = round.finishedAt
    }
}
