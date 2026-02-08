package infrastructure.api.grpc.mapper

import com.nekgamebling.game.dto.*
import domain.aggregator.AggregatorInfo
import domain.collection.model.Collection
import domain.common.value.Aggregator
import domain.common.value.Platform
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.provider.model.Provider
import domain.session.model.Round
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun Game.toProto(providerIdentity: String): GameDto = GameDto.newBuilder()
    .setIdentity(identity)
    .setName(name)
    .setProviderIdentity(providerIdentity)
    .setImages(images.toProto())
    .setBonusBetEnable(bonusBetEnable)
    .setBonusWageringEnable(bonusWageringEnable)
    .addAllTags(tags)
    .setActive(active)
    .build()

fun GameVariant.toProto(gameIdentity: String? = null): GameVariantDto = GameVariantDto.newBuilder()
    .setSymbol(symbol)
    .apply { gameIdentity?.let { setGameIdentity(it) } }
    .setName(name)
    .setProviderName(providerName)
    .setAggregator(aggregator.toProto())
    .setFreeSpinEnable(freeSpinEnable)
    .setFreeChipEnable(freeChipEnable)
    .setJackpotEnable(jackpotEnable)
    .setDemoEnable(demoEnable)
    .setBonusBuyEnable(bonusBuyEnable)
    .addAllLocales(locales.map { it.value })
    .addAllPlatforms(platforms.map { it.toProto() })
    .setPlayLines(playLines)
    .build()

fun Provider.toProto(aggregatorIdentity: String? = null): ProviderDto = ProviderDto.newBuilder()
    .setIdentity(identity)
    .setName(name)
    .setImages(images.toProto())
    .setOrder(order)
    .apply { aggregatorIdentity?.let { setAggregatorIdentity(it) } }
    .setActive(active)
    .build()

fun AggregatorInfo.toProto(): AggregatorInfoDto = AggregatorInfoDto.newBuilder()
    .setIdentity(identity)
    .putAllConfig(config)
    .setAggregator(aggregator.toProto())
    .setActive(active)
    .build()

fun Collection.toProto(): CollectionDto = CollectionDto.newBuilder()
    .setIdentity(identity)
    .setName(name.toProto())
    .setActive(active)
    .setOrder(order)
    .build()

fun shared.value.ImageMap.toProto(): ImageMapDto = ImageMapDto.newBuilder()
    .putAllImages(this.data)
    .build()

fun shared.value.LocaleName.toProto(): LocaleNameDto = LocaleNameDto.newBuilder()
    .putAllValues(this.data)
    .build()

fun Aggregator.toProto(): AggregatorTypeDto = when (this) {
    Aggregator.ONEGAMEHUB -> AggregatorTypeDto.AGGREGATOR_ONEGAMEHUB
    Aggregator.PRAGMATIC -> AggregatorTypeDto.AGGREGATOR_PRAGMATIC
    Aggregator.PATEPLAY -> AggregatorTypeDto.AGGREGATOR_PATEPLAY
}

fun AggregatorTypeDto.toDomain(): Aggregator = when (this) {
    AggregatorTypeDto.AGGREGATOR_ONEGAMEHUB -> Aggregator.ONEGAMEHUB
    AggregatorTypeDto.AGGREGATOR_PRAGMATIC -> Aggregator.PRAGMATIC
    AggregatorTypeDto.AGGREGATOR_PATEPLAY -> Aggregator.PATEPLAY
    AggregatorTypeDto.AGGREGATOR_UNSPECIFIED, AggregatorTypeDto.UNRECOGNIZED ->
        throw IllegalArgumentException("Invalid aggregator type: $this")
}

fun Platform.toProto(): PlatformDto = when (this) {
    Platform.DESKTOP -> PlatformDto.PLATFORM_DESKTOP
    Platform.MOBILE -> PlatformDto.PLATFORM_MOBILE
    Platform.DOWNLOAD -> PlatformDto.PLATFORM_DOWNLOAD
}

fun LocalDateTime.toProto(): TimestampDto {
    val instant = this.toInstant(TimeZone.UTC)
    return TimestampDto.newBuilder()
        .setSeconds(instant.epochSeconds)
        .setNanos(instant.nanosecondsOfSecond)
        .build()
}

fun TimestampDto.toDomain(): LocalDateTime {
    val instant = Instant.fromEpochSeconds(seconds, nanos)
    return instant.toLocalDateTime(TimeZone.UTC)
}

fun Round.toProto(): RoundDto {
    val builder = RoundDto.newBuilder()
        .setId(id.toString())
        .setSessionId(sessionId.toString())
        .setGameId(gameId.toString())
        .setExtId(extId)
        .setFinished(finished)
        .setCreatedAt(createdAt.toProto())
    finishedAt?.let { builder.setFinishedAt(it.toProto()) }
    return builder.build()
}
