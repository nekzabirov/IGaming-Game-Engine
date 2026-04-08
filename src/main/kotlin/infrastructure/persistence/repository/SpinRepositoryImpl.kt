package infrastructure.persistence.repository

import domain.repository.ISpinRepository
import domain.model.Spin
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.SpinEntity
import infrastructure.persistence.mapper.SpinMapper.toDomain
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update

class SpinRepositoryImpl : ISpinRepository {

    override suspend fun save(spin: Spin): Spin = dbTransaction {
        if (spin.id == Long.MIN_VALUE) {
            val id = SpinTable.insertAndGetId { it.fromDomain(spin) }
            spin.copy(id = id.value)
        } else {
            SpinTable.update({ SpinTable.id eq spin.id }) { it.fromDomain(spin) }
            spin
        }
    }

    override suspend fun findById(id: Long): Spin? = dbRead {
        SpinEntity.findById(id)?.toDomain()
    }

    override suspend fun findByExternalId(externalId: String): Spin? = dbRead {
        SpinEntity.find { SpinTable.externalId eq externalId }
            .firstOrNull()?.toDomain()
    }

    private fun UpdateBuilder<*>.fromDomain(spin: Spin) {
        this[SpinTable.externalId] = spin.externalId.value
        this[SpinTable.round] = spin.round.id
        this[SpinTable.reference] = spin.reference?.id
        this[SpinTable.type] = spin.type
        this[SpinTable.amount] = spin.amount.value
        this[SpinTable.realAmount] = spin.realAmount.value
        this[SpinTable.bonusAmount] = spin.bonusAmount.value
    }
}
