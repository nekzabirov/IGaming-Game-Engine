package infrastructure.aggregator.pateplay.client.dto

import kotlinx.serialization.Serializable

data class CreateFreespinRequestDto(
    val referenceId: String,

    val playerId: String,

    val currency: String,

    val ttlSeconds: Long,

    val gameSymbol: String,

    val stake: String,

    val rounds: Int,

    val expiresAt: String
)

@Serializable
data class CreateFreespinBodyDto(
    val bonuses: List<FreespinBonusDto>
)

@Serializable
data class FreespinBonusDto(
    val bonusRef: String,

    val playerId: String,

    val siteCode: String,

    val currency: String,

    val type: String = "bets",

    val config: FreespinConfigDto,

    val timeExpires: String
)

@Serializable
data class FreespinConfigDto(
    val ttl: Long,

    val games: List<String>,

    val stake: String,

    val bets: Int
)
