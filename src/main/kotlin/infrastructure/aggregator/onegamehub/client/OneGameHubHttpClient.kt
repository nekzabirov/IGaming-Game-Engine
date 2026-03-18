package infrastructure.aggregator.onegamehub.client

import domain.model.Platform
import infrastructure.aggregator.onegamehub.OneGameHubConfig
import infrastructure.aggregator.onegamehub.client.dto.CancelFreespinDto
import infrastructure.aggregator.onegamehub.client.dto.CreateFreespinDto
import infrastructure.aggregator.onegamehub.client.dto.GameDto
import infrastructure.aggregator.onegamehub.client.dto.GameUrlDto
import infrastructure.aggregator.onegamehub.client.dto.ResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class OneGameHubHttpClient(private val config: OneGameHubConfig) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                coerceInputValues = true
            })
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

    private val address = "https://${config.gateway}/integrations/${config.partner}/rpc"

    suspend fun listGames(): ResponseDto<List<GameDto>> {
        return client.get(address) {
            setAction("available_games")
        }.body()
    }

    suspend fun getLaunchUrl(
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: String,
        platform: Platform,
        currency: String,
        lobbyUrl: String,
        demo: Boolean
    ): ResponseDto<GameUrlDto> {
        return client.get(address) {
            setAction(if (demo) "demo_play" else "real_play")

            parameter("game_id", gameSymbol)

            if (!demo) {
                parameter("player_id", playerId)
            }

            parameter("currency", currency)
            parameter("mobile", if (platform == Platform.MOBILE) "1" else "0")
            parameter("language", locale)

            if (sessionToken.isNotBlank()) {
                parameter("extra", sessionToken)
            }

            parameter("return_url", lobbyUrl)
            parameter("deposit_url", lobbyUrl)
        }.body()
    }

    suspend fun createFreespin(payload: CreateFreespinDto): ResponseDto<String> {
        return client.post(address) {
            setAction("freerounds_create")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    suspend fun cancelFreespin(payload: CancelFreespinDto): ResponseDto<String> {
        return client.post(address) {
            setAction("freerounds_cancel")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    private fun HttpRequestBuilder.setAction(action: String) {
        parameter("action", action)
        parameter("secret", config.secret)
    }
}
