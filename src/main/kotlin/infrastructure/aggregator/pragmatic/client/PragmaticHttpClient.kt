package infrastructure.aggregator.pragmatic.client

import infrastructure.aggregator.pragmatic.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.dto.CreateFreespinDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinGameListDto
import infrastructure.aggregator.pragmatic.client.dto.GameDto
import infrastructure.aggregator.pragmatic.client.dto.GameUrlResponseDto
import infrastructure.aggregator.pragmatic.client.dto.GamesResponseDto
import infrastructure.aggregator.pragmatic.client.dto.LaunchUrlRequestDto
import infrastructure.aggregator.pragmatic.client.dto.ResponseDto
import domain.model.Platform
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
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class PragmaticHttpClient(private val config: PragmaticConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
        coerceInputValues = true
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

    private val baseUrl: String
        get() = "https://${config.gatewayUrl}"

    suspend fun listGames(): List<GameDto> {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "options" to "GetFrbDetails,GetLines,GetDataTypes,GetFeatures,GetFcDetails,GetStudio"
        )
        params["hash"] = generateHash(params)

        val response = client.post("$baseUrl/IntegrationService/v3/http/CasinoGameAPI/getCasinoGames") {
            contentType(ContentType.Application.FormUrlEncoded)
            accept(ContentType.Application.Json)
            setBody(FormDataContent(Parameters.build {
                params.forEach { (key, value) -> append(key, value) }
            }))
        }

        check(response.status.isSuccess()) {
            "Pragmatic listGames failed with HTTP status ${response.status}"
        }

        val body: GamesResponseDto = response.body()

        check(body.success) {
            "Pragmatic API error: ${body.error} - ${body.description}"
        }

        return body.gameList ?: emptyList()
    }

    suspend fun getLaunchUrl(payload: LaunchUrlRequestDto): String {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "externalPlayerId" to payload.playerId,
            "token" to payload.sessionToken,
            "language" to payload.locale,
            "symbol" to payload.gameSymbol,
            "currency" to payload.currency,
            "platform" to payload.platform.toPragmaticPlatform(),
            "playMode" to if (payload.demo) "DEMO" else "REAL",
            "lobbyUrl" to payload.lobbyUrl,
            "cashierUrl" to payload.lobbyUrl
        )
        params["hash"] = generateHash(params)

        val response = client.post("$baseUrl/IntegrationService/v3/http/CasinoGameAPI/game/url/") {
            contentType(ContentType.Application.FormUrlEncoded)
            accept(ContentType.Application.Json)
            setBody(FormDataContent(Parameters.build {
                params.forEach { (key, value) -> append(key, value) }
            }))
        }

        check(response.status.isSuccess()) {
            "Pragmatic getLaunchUrl failed with HTTP status ${response.status}"
        }

        val body: GameUrlResponseDto = response.body()

        check(body.success) {
            "Pragmatic API error: ${body.error} - ${body.description}"
        }

        return requireNotNull(body.gameURL?.takeIf { it.isNotBlank() }) {
            "Pragmatic returned empty game URL"
        }
    }

    suspend fun createFreespin(payload: CreateFreespinDto) {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "bonusCode" to payload.bonusCode,
            "playerId" to payload.playerId,
            "currency" to payload.currency,
            "rounds" to payload.rounds.toString(),
            "startDate" to payload.startTimestamp.toString(),
            "expirationDate" to payload.expirationTimestamp.toString()
        )
        params["hash"] = generateHash(params)

        val jsonBody = FreespinGameListDto(gameList = payload.gameList)

        val url = buildString {
            append("$baseUrl/IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/player/create")
            append("?")
            append(params.entries.joinToString("&") { "${it.key}=${it.value}" })
        }

        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(json.encodeToString(jsonBody))
        }

        check(response.status.isSuccess()) {
            "Pragmatic createFreespin failed with HTTP status ${response.status}"
        }

        val body: ResponseDto = response.body()

        check(body.success) {
            "Pragmatic API error: ${body.error} - ${body.description}"
        }
    }

    suspend fun cancelFreespin(bonusCode: String) {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "bonusCode" to bonusCode
        )
        params["hash"] = generateHash(params)

        val response = client.post("$baseUrl/IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/cancel/") {
            contentType(ContentType.Application.FormUrlEncoded)
            accept(ContentType.Application.Json)
            setBody(FormDataContent(Parameters.build {
                params.forEach { (key, value) -> append(key, value) }
            }))
        }

        check(response.status.isSuccess()) {
            "Pragmatic cancelFreespin failed with HTTP status ${response.status}"
        }

        val body: ResponseDto = response.body()

        check(body.success) {
            "Pragmatic API error: ${body.error} - ${body.description}"
        }
    }

    private fun generateHash(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val queryString = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val dataToHash = queryString + config.secretKey

        return MessageDigest.getInstance("MD5")
            .digest(dataToHash.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun Platform.toPragmaticPlatform(): String = when (this) {
        Platform.DESKTOP -> "DESKTOP"
        Platform.MOBILE -> "MOBILE"
        Platform.DOWNLOAD -> "DOWNLOAD"
    }
}
