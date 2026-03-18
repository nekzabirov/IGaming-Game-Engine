package infrastructure.aggregator.pateplay.client

import infrastructure.aggregator.pateplay.PateplayConfig
import infrastructure.aggregator.pateplay.client.dto.CancelFreespinBodyDto
import infrastructure.aggregator.pateplay.client.dto.CreateFreespinBodyDto
import infrastructure.aggregator.pateplay.client.dto.CreateFreespinRequestDto
import infrastructure.aggregator.pateplay.client.dto.FreespinBonusDto
import infrastructure.aggregator.pateplay.client.dto.FreespinConfigDto
import infrastructure.aggregator.pateplay.client.dto.PateplayResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class PateplayHttpClient(private val config: PateplayConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    private val gatewayBaseUrl: String
        get() = "https://${config.gatewayUrl}"

    suspend fun createFreespin(payload: CreateFreespinRequestDto) {
        val body = CreateFreespinBodyDto(
            bonuses = listOf(
                FreespinBonusDto(
                    bonusRef = payload.referenceId,
                    playerId = payload.playerId,
                    siteCode = config.siteCode,
                    currency = payload.currency,
                    type = "bets",
                    config = FreespinConfigDto(
                        ttl = payload.ttlSeconds,
                        games = listOf(payload.gameSymbol),
                        stake = payload.stake,
                        bets = payload.rounds
                    ),
                    timeExpires = payload.expiresAt
                )
            )
        )

        postGateway("/bonuses/create", body)
    }

    suspend fun cancelFreespin(bonusId: Long) {
        val body = CancelFreespinBodyDto(
            ids = listOf(bonusId),
            reason = "Bonus cancelled by operator",
            force = false
        )

        postGateway("/bonuses/cancel", body)
    }

    private suspend inline fun <reified T> postGateway(path: String, payload: T) {
        val jsonBody = json.encodeToString(payload)
        val hmac = computeHmacSha256(jsonBody, config.gatewayApiSecret)

        val response = client.post("$gatewayBaseUrl$path") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header("x-api-key", config.gatewayApiKey)
            header("x-api-hmac", hmac)
            setBody(jsonBody)
        }

        check(response.status.isSuccess()) {
            "PatePlay request to $path failed with HTTP status ${response.status}"
        }

        val responseBody: PateplayResponseDto = response.body()

        check(responseBody.isSuccess) {
            "PatePlay API error: ${responseBody.error?.code} - ${responseBody.error?.message}"
        }
    }

    private fun computeHmacSha256(data: String, secret: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(secretKeySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }
}
