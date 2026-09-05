package infrastructure.gamehub

import domain.exception.conflict.DemoNotSupportedException
import domain.exception.conflict.FreespinNotSupportedException
import domain.exception.notfound.CasinoGameNotFoundException
import domain.exception.notfound.CasinoGameNotRoutedException
import domain.exception.system.GameHubUnavailableException
import domain.model.Platform
import gamehub.v1.Gateway
import gamehub.v1.GatewayServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.stub.MetadataUtils
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Operator → hub gRPC client for `GatewayService` — the only way casino-engine reaches a vendor
 * now, for BOTH products. Every call carries `x-operator-id`/`x-operator-key`; every failure comes
 * back with the machine-readable reason in the `x-error-code` trailer (`docs/ERRORS.md` in the hub
 * repo) — the status alone is not specific enough (`NO_ROUTE` and `UNSUPPORTED` both read as
 * `FAILED_PRECONDITION`).
 */
class GameHubClient(channel: ManagedChannel, config: GameHubConfig) {

    private val stub: GatewayServiceGrpc.GatewayServiceBlockingStub =
        GatewayServiceGrpc.newBlockingStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders(config)))

    suspend fun listCasino(): Gateway.ListCasinoResponse =
        call { it.listCasino(Gateway.ListCasinoRequest.getDefaultInstance()) }

    suspend fun launchCasino(
        game: String,
        playerId: String,
        currency: String,
        locale: String,
        lobbyUrl: String,
        platform: Platform,
        freespinId: String?,
    ): String {
        val request = Gateway.LaunchCasinoRequest.newBuilder()
            .setGame(game)
            .setPlayerId(playerId)
            .setCurrency(currency)
            .setLocale(locale)
            .setLobbyUrl(lobbyUrl)
            .setPlatform(platform.toProto())
            .apply { freespinId?.let { setFreespinId(it) } }
            .build()

        return call { it.launchCasino(request) }.url
    }

    suspend fun launchCasinoDemo(
        game: String,
        currency: String,
        locale: String,
        lobbyUrl: String,
        platform: Platform,
    ): String {
        val request = Gateway.LaunchCasinoDemoRequest.newBuilder()
            .setGame(game)
            .setCurrency(currency)
            .setLocale(locale)
            .setLobbyUrl(lobbyUrl)
            .setPlatform(platform.toProto())
            .build()

        return call { it.launchCasinoDemo(request) }.url
    }

    suspend fun freespinPresets(game: String): Gateway.FreespinPresetsResponse {
        val request = Gateway.FreespinPresetsRequest.newBuilder().setGame(game).build()
        return call { it.freespinPresetsCasino(request) }
    }

    /** [amount] is the stake per free round in nano — the hub speaks the same unit as the wallet,
     *  so it travels through unscaled. Returns the hub's own grant id. */
    suspend fun createFreespin(
        game: String,
        playerId: String,
        amount: Long,
        count: Int,
        currency: String,
        presets: Map<String, String>,
        reference: String,
        durationSeconds: Long,
    ) {
        val request = Gateway.CreateFreespinRequest.newBuilder()
            .setGame(game)
            .setPlayerId(playerId)
            .setAmount(amount)
            .setCount(count)
            .setCurrency(currency)
            .putAllPresets(presets)
            // Ссылка вызывающего едет насквозь: хаб отдаёт её вендору и ею же называет грант в
            // кошельковых вызовах, так что она вернётся сюда на спине.
            .setReference(reference)
            .setDurationSeconds(durationSeconds)
            .build()

        call { it.createFreespin(request) }
    }

    /** Отмена по ссылке вызывающего: своего номера гранта casino-engine не хранит. */
    suspend fun cancelFreespin(reference: String) {
        val request = Gateway.CancelFreespinRequest.newBuilder().setReference(reference).build()
        call { it.cancelFreespin(request) }
    }

    suspend fun openSportbook(playerId: String?, currency: String, locale: String): Gateway.OpenSportbookResponse {
        val request = Gateway.OpenSportbookRequest.newBuilder()
            .apply { playerId?.let { setPlayerId(it) } }
            .setCurrency(currency)
            .setLocale(locale)
            .build()

        return call { it.openSportbook(request) }
    }

    private suspend fun <T> call(block: (GatewayServiceGrpc.GatewayServiceBlockingStub) -> T): T =
        try {
            withContext(Dispatchers.IO) {
                block(stub.withDeadlineAfter(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            }
        } catch (e: StatusRuntimeException) {
            throw translate(e)
        }

    /**
     * The gRPC status is deliberately coarse on the hub's side — several codes share
     * `FAILED_PRECONDITION` — so the trailer is the real API. A code with no casino-engine
     * equivalent is rethrown untouched rather than flattened into a wrong domain exception.
     */
    private fun translate(e: StatusRuntimeException): Throwable =
        when (e.trailers?.get(ERROR_CODE_KEY)) {
            ERROR_NOT_FOUND -> CasinoGameNotFoundException()
            ERROR_NO_ROUTE -> CasinoGameNotRoutedException()
            ERROR_UNSUPPORTED -> FreespinNotSupportedException()
            ERROR_UNAUTHENTICATED, ERROR_INTERNAL -> GameHubUnavailableException(e.message)
            else -> e
        }

    private fun Platform.toProto(): Gateway.Platform =
        if (this == Platform.MOBILE) Gateway.Platform.PLATFORM_MOBILE else Gateway.Platform.PLATFORM_DESKTOP

    private companion object {
        const val CALL_TIMEOUT_SECONDS = 30L

        const val ERROR_NOT_FOUND = "NOT_FOUND"

        const val ERROR_NO_ROUTE = "NO_ROUTE"

        const val ERROR_UNSUPPORTED = "UNSUPPORTED"

        const val ERROR_UNAUTHENTICATED = "UNAUTHENTICATED"

        const val ERROR_INTERNAL = "INTERNAL"

        val ERROR_CODE_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

        fun authHeaders(config: GameHubConfig): Metadata = Metadata().apply {
            put(Metadata.Key.of("x-operator-id", Metadata.ASCII_STRING_MARSHALLER), config.operatorId)
            put(Metadata.Key.of("x-operator-key", Metadata.ASCII_STRING_MARSHALLER), config.operatorKey)
        }
    }
}
