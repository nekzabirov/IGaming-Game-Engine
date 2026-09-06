package dto

import com.google.protobuf.ListValue
import com.google.protobuf.NullValue
import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.nekgamebling.game.v1.CasinoGameDto
import com.nekgamebling.game.v1.CasinoGamePageDto
import com.nekgamebling.game.v1.CasinoProviderDto
import com.nekgamebling.game.v1.CasinoRoundDto
import com.nekgamebling.game.v1.CollectionDto
import com.nekgamebling.game.v1.PlatformDto
import com.nekgamebling.game.v1.casinoGameDto
import com.nekgamebling.game.v1.casinoGamePageDto
import com.nekgamebling.game.v1.casinoProviderDto
import com.nekgamebling.game.v1.casinoRoundDto
import com.nekgamebling.game.v1.collectionDto
import db.CasinoGame
import db.CasinoGames
import db.CasinoProvider
import db.CasinoProviders
import db.CasinoRound
import db.Collection
import db.Pageable
import db.Platform
import org.jetbrains.exposed.sql.ResultRow

// Entity -> proto. Runs inside the transaction that loaded the entity (relations are lazy).

fun CasinoProvider.toDto(): CasinoProviderDto = casinoProviderDto {
    identity = this@toDto.identity
    name = this@toDto.name
    images.putAll(this@toDto.resolvedImages)
    order = this@toDto.sortOrder
    active = this@toDto.active
    blockedCountry.addAll(this@toDto.blockedCountry)
    tags.addAll(this@toDto.resolvedTags)
    customTags.addAll(this@toDto.customTags)
}

fun Collection.toDto(): CollectionDto = collectionDto {
    identity = this@toDto.identity
    name.putAll(this@toDto.name)
    tags.addAll(this@toDto.tags)
    images.putAll(this@toDto.images)
    active = this@toDto.active
    order = this@toDto.sortOrder
}

/**
 * `symbol`/`integration` stay on the wire for consumers that read them but carry no vendor
 * meaning any more; `images`/`tags` are the resolved (hub + local override) merge.
 */
fun CasinoGame.toDto(): CasinoGameDto = casinoGameDto {
    identity = this@toDto.identity
    name = this@toDto.name
    providerIdentity = this@toDto.provider.identity
    collectionIdentities.addAll(this@toDto.collections.map { it.identity })
    bonusBetEnable = this@toDto.bonusBetEnable
    bonusWageringEnable = this@toDto.bonusWageringEnable
    tags.addAll(this@toDto.resolvedTags)
    customTags.addAll(this@toDto.customTags)
    active = this@toDto.active
    images.putAll(this@toDto.resolvedImages)
    order = this@toDto.sortOrder
    rtp = this@toDto.rtp ?: 0.0
    symbol = this@toDto.identity
    integration = INTEGRATION
    freeSpinEnable = this@toDto.freeSpinEnable
    freeChipEnable = this@toDto.freeChipEnable
    jackpotEnable = this@toDto.jackpotEnable
    demoEnable = this@toDto.demoEnable
    bonusBuyEnable = this@toDto.bonusBuyEnable
    locales.addAll(this@toDto.locales)
    platforms.addAll(this@toDto.platforms.map { Platform.valueOf(it).toDto() })
    playLines = this@toDto.playLines
}

/**
 * A game read off a joined row (winners feed) — the column list of the query has to cover every
 * column read here AND the provider's. No collections: the feed never carried them.
 */
fun ResultRow.toCasinoGameDto(): CasinoGameDto = casinoGameDto {
    val row = this@toCasinoGameDto
    identity = row[CasinoGames.identity]
    name = row[CasinoGames.name]
    providerIdentity = row[CasinoProviders.identity]
    bonusBetEnable = row[CasinoGames.bonusBetEnable]
    bonusWageringEnable = row[CasinoGames.bonusWageringEnable]
    tags.addAll((row[CasinoGames.tags] + row[CasinoGames.customTags]).distinct())
    customTags.addAll(row[CasinoGames.customTags])
    active = row[CasinoGames.active]
    images.putAll(row[CasinoGames.images] + row[CasinoGames.customImages])
    order = row[CasinoGames.sortOrder]
    rtp = row[CasinoGames.rtp] ?: 0.0
    symbol = row[CasinoGames.identity]
    integration = INTEGRATION
    freeSpinEnable = row[CasinoGames.freeSpinEnable]
    freeChipEnable = row[CasinoGames.freeChipEnable]
    jackpotEnable = row[CasinoGames.jackpotEnable]
    demoEnable = row[CasinoGames.demoEnable]
    bonusBuyEnable = row[CasinoGames.bonusBuyEnable]
    locales.addAll(row[CasinoGames.locales])
    platforms.addAll(row[CasinoGames.platforms].map { Platform.valueOf(it).toDto() })
    playLines = row[CasinoGames.playLines]
}

/** `game` is left unset for a sportsbook leg — `has_game` is how a reader tells a bet from a spin. */
fun CasinoRound.toDto(): CasinoRoundDto = casinoRoundDto {
    id = this@toDto.id.value
    externalId = this@toDto.externalId
    this@toDto.freespinId?.let { freespinId = it }
    playerId = this@toDto.playerId
    this@toDto.game?.let { game = it.toDto() }
    currency = this@toDto.currency
    createdAt = this@toDto.createdAt.toString()
    this@toDto.finishedAt?.let { finishedAt = it.toString() }
}

/**
 * The shared paged listing shape: items plus the providers and collections they reference,
 * denormalized once instead of once per game. Joins back by identity.
 */
fun List<CasinoGame>.toPageDto(totalItems: Long): CasinoGamePageDto {
    val games = this
    val uniqueProviders = games.map { it.provider }.distinctBy { it.id }
    val uniqueCollections = games.flatMap { it.collections }.distinctBy { it.id }

    return casinoGamePageDto {
        items.addAll(games.map { it.toDto() })
        providers.addAll(uniqueProviders.map { it.toDto() })
        collections.addAll(uniqueCollections.map { it.toDto() })
        this.totalItems = totalItems.toInt()
    }
}

fun Platform.toDto(): PlatformDto = when (this) {
    Platform.DESKTOP -> PlatformDto.PLATFORM_DESKTOP
    Platform.MOBILE -> PlatformDto.PLATFORM_MOBILE
    Platform.DOWNLOAD -> PlatformDto.PLATFORM_DOWNLOAD
}

fun PlatformDto.toPlatform(): Platform = when (this) {
    PlatformDto.PLATFORM_DESKTOP -> Platform.DESKTOP
    PlatformDto.PLATFORM_MOBILE -> Platform.MOBILE
    PlatformDto.PLATFORM_DOWNLOAD -> Platform.DOWNLOAD
    PlatformDto.PLATFORM_UNSPECIFIED, PlatformDto.UNRECOGNIZED -> Platform.DESKTOP
}

fun pageable(pageNum: Int, pageSize: Int): Pageable = Pageable(pageNum, pageSize)

private const val INTEGRATION = "GAMEHUB"

// Freeform preset maps (freespin presets) <-> protobuf Struct.

fun Map<String, Any>.toStruct(): Struct {
    val builder = Struct.newBuilder()
    for ((key, value) in this) builder.putFields(key, value.toValue())
    return builder.build()
}

fun Struct.toMap(): Map<String, Any> = fieldsMap.mapValues { (_, value) -> value.toAny() }

@Suppress("UNCHECKED_CAST")
private fun Any?.toValue(): Value {
    val builder = Value.newBuilder()
    when (this) {
        null -> builder.nullValue = NullValue.NULL_VALUE
        is String -> builder.stringValue = this
        is Number -> builder.numberValue = toDouble()
        is Boolean -> builder.boolValue = this
        is Map<*, *> -> builder.structValue = (this as Map<String, Any>).toStruct()
        is List<*> -> builder.listValue = ListValue.newBuilder().addAllValues(map { it.toValue() }).build()
        else -> builder.stringValue = toString()
    }
    return builder.build()
}

private fun Value.toAny(): Any = when (kindCase) {
    Value.KindCase.STRING_VALUE -> stringValue
    Value.KindCase.NUMBER_VALUE -> numberValue
    Value.KindCase.BOOL_VALUE -> boolValue
    Value.KindCase.STRUCT_VALUE -> structValue.toMap()
    Value.KindCase.LIST_VALUE -> listValue.valuesList.map { it.toAny() }
    Value.KindCase.NULL_VALUE, Value.KindCase.KIND_NOT_SET, null -> ""
}
