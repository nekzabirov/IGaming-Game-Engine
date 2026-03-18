package infrastructure.aggregator.pragmatic.client.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDto(
    @SerialName("gameID")
    val gameId: String,

    val gameName: String,

    val gameTypeID: String? = null,

    @SerialName("frbAvailable")
    val freespinEnable: Boolean = false,

    @SerialName("fcAvailable")
    val freeChipEnable: Boolean = false,

    @SerialName("demoGameAvailable")
    val demoEnable: Boolean = false,

    val lines: Int = 0,

    val platform: String? = null,

    val features: List<String>? = null
)
