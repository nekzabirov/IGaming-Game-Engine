package events

import db.CasinoGame
import db.CasinoProvider
import db.CasinoRound
import db.Collection
import db.Platform
import db.Spin

// Entity -> wire payload. Each walks the entity's relations, so it runs INSIDE the transaction that
// loaded the entity; the payload it returns is plain data and outlives the transaction.

fun CasinoProvider.toPayload(): ProviderPayload = ProviderPayload(
    identity = identity,
    name = name,
    images = images,
    customImages = customImages,
    order = sortOrder,
    active = active,
    blockedCountry = blockedCountry,
    tags = tags,
    customTags = customTags,
)

fun Collection.toPayload(): CollectionPayload = CollectionPayload(
    identity = identity,
    name = name,
    tags = tags,
    images = images,
    active = active,
    order = sortOrder,
)

fun CasinoGame.toPayload(): GamePayload = GamePayload(
    identity = identity,
    name = name,
    provider = provider.toPayload(),
    collections = collections.map { it.toPayload() },
    bonusBetEnable = bonusBetEnable,
    bonusWageringEnable = bonusWageringEnable,
    tags = tags,
    rtp = rtp,
    freeSpinEnable = freeSpinEnable,
    freeChipEnable = freeChipEnable,
    jackpotEnable = jackpotEnable,
    demoEnable = demoEnable,
    bonusBuyEnable = bonusBuyEnable,
    locales = locales,
    platforms = platforms.map { Platform.valueOf(it) },
    playLines = playLines,
    active = active,
    images = images,
    customImages = customImages,
    customTags = customTags,
    order = sortOrder,
)

fun CasinoRound.toPayload(): RoundPayload = RoundPayload(
    id = id.value,
    externalId = externalId,
    freespinId = freespinId,
    playerId = playerId,
    game = game?.toPayload(),
    currency = currency,
    createdAt = createdAt,
    finishedAt = finishedAt,
)

fun Spin.toPayload(): SpinPayload = SpinPayload(
    id = id.value,
    externalId = externalId,
    round = round.toPayload(),
    reference = reference?.toPayload(),
    type = type,
    amount = amount,
    realAmount = realAmount,
    bonusAmount = bonusAmount,
)
