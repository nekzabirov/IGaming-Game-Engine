package db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

// The DAO entities ARE the model: there is no second set of classes they are copied into. Read and
// write them inside dbTransaction/dbRead only — a relation touched outside a transaction throws.

class CasinoProvider(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CasinoProvider>(CasinoProviders) {
        fun findByIdentity(identity: String): CasinoProvider? =
            find { CasinoProviders.identity eq identity }.firstOrNull()
    }

    var identity by CasinoProviders.identity
    var name by CasinoProviders.name
    var images by CasinoProviders.images
    var customImages by CasinoProviders.customImages
    var sortOrder by CasinoProviders.sortOrder
    var active by CasinoProviders.active
    var blockedCountry by CasinoProviders.blockedCountry
    var tags by CasinoProviders.tags
    var customTags by CasinoProviders.customTags

    /** What the wire shows: the hub's artwork with our overrides on top, per key. */
    val resolvedImages: Map<String, String> get() = images + customImages

    /** The hub's tags plus ours, deduplicated, hub order first. */
    val resolvedTags: List<String> get() = (tags + customTags).distinct()
}

class CasinoGame(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CasinoGame>(CasinoGames) {
        const val DEFAULT_RTP = 96.0

        fun findByIdentity(identity: String): CasinoGame? =
            find { CasinoGames.identity eq identity }.firstOrNull()
    }

    var identity by CasinoGames.identity
    var name by CasinoGames.name
    var provider by CasinoProvider referencedOn CasinoGames.provider
    var bonusBetEnable by CasinoGames.bonusBetEnable
    var bonusWageringEnable by CasinoGames.bonusWageringEnable
    var tags by CasinoGames.tags
    var rtp by CasinoGames.rtp
    var freeSpinEnable by CasinoGames.freeSpinEnable
    var freeChipEnable by CasinoGames.freeChipEnable
    var jackpotEnable by CasinoGames.jackpotEnable
    var demoEnable by CasinoGames.demoEnable
    var bonusBuyEnable by CasinoGames.bonusBuyEnable
    var locales by CasinoGames.locales
    var platforms by CasinoGames.platforms
    var playLines by CasinoGames.playLines
    var active by CasinoGames.active
    var images by CasinoGames.images
    var customImages by CasinoGames.customImages
    var customTags by CasinoGames.customTags
    var sortOrder by CasinoGames.sortOrder
    var collections by Collection via CasinoGameCollections

    val resolvedImages: Map<String, String> get() = images + customImages

    val resolvedTags: List<String> get() = (tags + customTags).distinct()

    fun supportsPlatform(platform: Platform): Boolean = platforms.contains(platform.name)

    fun supportsLocale(locale: String): Boolean = locales.contains(locale)
}

class Collection(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Collection>(Collections) {
        fun findByIdentity(identity: String): Collection? =
            find { Collections.identity eq identity }.firstOrNull()
    }

    var identity by Collections.identity
    var name by Collections.name
    var tags by Collections.tags
    var images by Collections.images
    var active by Collections.active
    var sortOrder by Collections.sortOrder
}

class CasinoRound(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CasinoRound>(CasinoRounds) {
        fun findByExternalId(externalId: String): CasinoRound? =
            find { CasinoRounds.externalId eq externalId }.firstOrNull()
    }

    var externalId by CasinoRounds.externalId
    var freespinId by CasinoRounds.freespinId
    var playerId by CasinoRounds.playerId
    var game by CasinoGame optionalReferencedOn CasinoRounds.game
    var currency by CasinoRounds.currency
    var createdAt by CasinoRounds.createdAt
    var finishedAt by CasinoRounds.finishedAt

    val isFinished: Boolean get() = finishedAt != null
}

class Spin(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Spin>(Spins) {
        fun findByExternalId(externalId: String): Spin? =
            find { Spins.externalId eq externalId }.firstOrNull()

        /**
         * The bet of [roundId] paid with bonus money, if any. A win lands in the pool its bet came
         * from; bet and win arrive as separate legs, and the round is what they share.
         */
        fun findBonusPlace(roundId: Long): Spin? = find {
            (Spins.round eq roundId) and (Spins.type eq SpinType.PLACE) and (Spins.bonusAmount greaterEq 1L)
        }.firstOrNull()
    }

    var externalId by Spins.externalId
    var round by CasinoRound referencedOn Spins.round
    // Raw foreign keys next to the relations: a new row is written with ids already known, without
    // fetching the referenced entities only to hand them back to Exposed.
    var roundId by Spins.round
    var reference by Spin optionalReferencedOn Spins.reference
    var referenceId by Spins.reference
    var type by Spins.type
    var amount by Spins.amount
    var realAmount by Spins.realAmount
    var bonusAmount by Spins.bonusAmount
}
