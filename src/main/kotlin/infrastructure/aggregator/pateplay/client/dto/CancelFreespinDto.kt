package infrastructure.aggregator.pateplay.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class CancelFreespinBodyDto(
    val ids: List<Long>,

    val reason: String,

    val force: Boolean
)
