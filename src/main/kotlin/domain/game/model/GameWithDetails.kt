package domain.game.model

import domain.aggregator.AggregatorInfo
import domain.provider.model.Provider
import shared.value.ImageMap
import domain.common.value.Locale
import domain.common.value.Platform
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Full game entity with all related details (provider, aggregator, variant info).
 * This is a read model used for queries.
 */
@Serializable
data class GameWithDetails(
    @Serializable(with = shared.serializer.UUIDSerializer::class)
    val id: UUID,
    val identity: String,
    val name: String,
    val images: ImageMap,
    val bonusBetEnable: Boolean = true,
    val bonusWageringEnable: Boolean = true,
    val tags: List<String> = emptyList(),
    val symbol: String,
    val freeSpinEnable: Boolean,
    val freeChipEnable: Boolean,
    val jackpotEnable: Boolean,
    val demoEnable: Boolean,
    val bonusBuyEnable: Boolean,
    val locales: List<Locale>,
    val platforms: List<Platform>,
    val playLines: Int,
    val provider: Provider,
    val aggregator: AggregatorInfo
) {
    fun toGame() = Game(
        id = id,
        identity = identity,
        name = name,
        providerId = provider.id,
        images = images,
        bonusBetEnable = bonusBetEnable,
        bonusWageringEnable = bonusWageringEnable,
        tags = tags,
        active = true
    )

    fun supportsLocale(locale: Locale): Boolean = locales.contains(locale)
    fun supportsPlatform(platform: Platform): Boolean = platforms.contains(platform)
}
