package infrastructure.aggregator.pateplay.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class PateplayErrorDto(
    val code: String? = null,

    val message: String? = null
)

@Serializable
data class PateplayResponseDto(
    val error: PateplayErrorDto? = null
) {
    val isSuccess: Boolean get() = error == null
}
