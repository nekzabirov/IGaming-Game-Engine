package infrastructure.handler.game

import application.query.game.GameFilter
import infrastructure.persistence.table.GameTable
import infrastructure.persistence.table.GameVariantTable
import infrastructure.persistence.table.ProviderTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.or

fun GameFilter.toCondition(): Op<Boolean> {
    val conditions = buildList<Op<Boolean>> {
        if (query.isNotBlank()) {
            val pattern = "%${query.lowercase()}%"
            add(Op.build {
                (GameTable.identity like pattern) or (GameTable.name like pattern)
            })
        }

        active?.let {
            add(Op.build { GameTable.active eq it })
        }

        bonusBetEnable?.let {
            add(Op.build { GameTable.bonusBetEnable eq it })
        }

        bonusWageringEnabled?.let {
            add(Op.build { GameTable.bonusWageringEnable eq it })
        }

        provider?.let { providerIdentity ->
            add(Op.build {
                GameTable.provider inSubQuery (
                    ProviderTable
                        .select(ProviderTable.id)
                        .where { ProviderTable.identity eq providerIdentity.value }
                )
            })
        }

        val variantConditions = buildList<Op<Boolean>> {
            freeSpinEnable?.let {
                add(Op.build { GameVariantTable.freeSpinEnable eq it })
            }
            freeChipEnable?.let {
                add(Op.build { GameVariantTable.freeChipEnable eq it })
            }
            jackpotEnable?.let {
                add(Op.build { GameVariantTable.jackpotEnable eq it })
            }
            demoEnable?.let {
                add(Op.build { GameVariantTable.demoEnable eq it })
            }
            bonusBuyEnable?.let {
                add(Op.build { GameVariantTable.bonusBuyEnable eq it })
            }
        }

        if (variantConditions.isNotEmpty()) {
            val variantCondition = variantConditions.reduce { acc, op -> acc and op }
            add(exists(
                GameVariantTable
                    .select(GameVariantTable.id)
                    .where {
                        (GameVariantTable.game eq GameTable.id) and variantCondition
                    }
            ))
        }

        if (inTags.isNotEmpty()) {
            add(inTags.map { tag ->
                Op.build { GameTable.tags.castTo<String>(TextColumnType()) like "%\"$tag\"%" }
            }.reduce { acc, op -> acc or op })
        }
    }

    return conditions.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
}
