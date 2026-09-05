package infrastructure.persistence.repository

import domain.exception.conflict.SpinAlreadyExistsException
import domain.model.Spin
import domain.model.SpinType
import domain.repository.ISpinRepository
import infrastructure.persistence.dbRead
import infrastructure.persistence.dbTransaction
import infrastructure.persistence.entity.SpinEntity
import infrastructure.persistence.mapper.SpinMapper.toDomain
import infrastructure.persistence.table.SpinTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import java.sql.SQLException

class SpinRepositoryImpl : ISpinRepository {

    private companion object {
        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
    }

    override suspend fun save(spin: Spin): Spin = dbTransaction {
        if (spin.id == Long.MIN_VALUE) {
            val id = try {
                SpinTable.insertAndGetId { it.fromDomain(spin) }
            } catch (e: ExposedSQLException) {
                if (e.isUniqueViolation()) throw SpinAlreadyExistsException() else throw e
            }
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

    override suspend fun findBonusPlaceByRound(roundId: Long): Spin? = dbRead {
        SpinEntity
            .find {
                (SpinTable.round eq roundId) and
                    (SpinTable.type eq SpinType.PLACE) and
                    (SpinTable.bonusAmount greaterEq 1L)
            }
            .firstOrNull()?.toDomain()
    }

    /** Postgres `unique_violation`; the driver exposes it as the SQLState, not as a typed error. */
    private fun ExposedSQLException.isUniqueViolation(): Boolean =
        generateSequence(cause) { it.cause }
            .filterIsInstance<SQLException>()
            .any { it.sqlState == UNIQUE_VIOLATION_SQL_STATE }

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
