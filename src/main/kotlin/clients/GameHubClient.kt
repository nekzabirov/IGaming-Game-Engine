package clients

import GameHubConfig
import db.Platform
import errors.CasinoGameNotFoundException
import errors.CasinoGameNotRoutedException
import errors.CasinoGameUnavailableException
import errors.FreespinNotSupportedException
import errors.GameHubUnavailableException
import gamehub.v1.Gateway
import gamehub.v1.GatewayServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider
import io.grpc.stub.MetadataUtils
import java.util.concurrent.TimeUnit

/** Everything casino-engine asks the hub for — the only way it reaches a vendor, both products. */
interface GameHub {

    suspend fun listCasino(): Gateway.ListCasinoResponse

    suspend fun launchCasino(
        game: String,
        playerId: String,
        currency: String,
        locale: String,
        lobbyUrl: String,
        platform: Platform,
        freespinId: String?,
    ): String

    suspend fun launchCasinoDemo(game: String, currency: String, locale: String, lobbyUrl: String, platform: Platform): String

    suspend fun freespinPresets(game: String): Gateway.FreespinPresetsResponse

    /** [amount] is the stake per free round in nano; [reference] is OUR name for the grant, which
     *  the hub hands to the vendor and echoes back on every wallet call of the grant. */
    suspend fun createFreespin(
        game: String,
        playerId: String,
        amount: Long,
        count: Int,
        currency: String,
        presets: Map<String, String>,
        reference: String,
        durationSeconds: Long,
    )

    /** Cancel by our reference: casino-engine keeps no grant id of its own. */
    suspend fun cancelFreespin(reference: String)

    suspend fun openSportbook(playerId: String?, currency: String, locale: String): Gateway.OpenSportbookResponse
}

/**
 * gRPC client for the hub's `GatewayService`. Every call carries `x-operator-id`/`x-operator-key`;
 * every refusal names its reason in the `x-error-code` trailer, which is the real API — several
 * codes share `FAILED_PRECONDITION`.
 *
 * TLS provider pinned to JDK: grpc-netty-shaded's bundled BoringSSL SIGSEGVs in this image, and
 * `Application.main` also sets `noOpenSsl` before any Netty SSL class loads.
 */
class GameHubClient(config: GameHubConfig) : GameHub {

    private val channel: ManagedChannel = NettyChannelBuilder
        .forAddress(config.host, config.port)
        .apply {
            if (config.plaintext) usePlaintext()
            else sslContext(GrpcSslContexts.forClient().sslProvider(SslProvider.JDK).build())
        }
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(true)
        .build()

    private val stub = GatewayServiceGrpcKt.GatewayServiceCoroutineStub(channel)
        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders(config)))

    override suspend fun listCasino(): Gateway.ListCasinoResponse =
        call { it.listCasino(Gateway.ListCasinoRequest.getDefaultInstance()) }

    override suspend fun launchCasino(
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

    override suspend fun launchCasinoDemo(
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

    override suspend fun freespinPresets(game: String): Gateway.FreespinPresetsResponse =
        call { it.freespinPresetsCasino(Gateway.FreespinPresetsRequest.newBuilder().setGame(game).build()) }

    override suspend fun createFreespin(
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
            .setReference(reference)
            .setDurationSeconds(durationSeconds)
            .build()

        call { it.createFreespin(request) }
    }

    override suspend fun cancelFreespin(reference: String) {
        call { it.cancelFreespin(Gateway.CancelFreespinRequest.newBuilder().setReference(reference).build()) }
    }

    override suspend fun openSportbook(playerId: String?, currency: String, locale: String): Gateway.OpenSportbookResponse {
        val request = Gateway.OpenSportbookRequest.newBuilder()
            .apply { playerId?.let { setPlayerId(it) } }
            .setCurrency(currency)
            .setLocale(locale)
            .build()

        return call { it.openSportbook(request) }
    }

    fun close() {
        channel.shutdown()
    }

    private suspend fun <T> call(block: suspend (GatewayServiceGrpcKt.GatewayServiceCoroutineStub) -> T): T =
        try {
            block(stub.withDeadlineAfter(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } catch (e: StatusException) {
            throw translate(e, e.trailers)
        } catch (e: StatusRuntimeException) {
            throw translate(e, e.trailers)
        }

    /**
     * Anything without a casino-engine equivalent — an unknown code, a transport failure, a
     * deadline — is a hub outage to the caller: INTERNAL, never the upstream status passed through.
     */
    private fun translate(e: Exception, trailers: Metadata?): Throwable =
        when (trailers?.get(ERROR_CODE_KEY)) {
            ERROR_NOT_FOUND -> CasinoGameNotFoundException()
            ERROR_NO_ROUTE -> CasinoGameNotRoutedException()
            ERROR_UNSUPPORTED -> FreespinNotSupportedException()
            // A refusal from the vendor behind the hub, not a hub outage.
            ERROR_VENDOR_REFUSED -> CasinoGameUnavailableException()
            else -> GameHubUnavailableException(e.message)
        }

    private fun Platform.toProto(): Gateway.Platform =
        if (this == Platform.MOBILE) Gateway.Platform.PLATFORM_MOBILE else Gateway.Platform.PLATFORM_DESKTOP

    private companion object {
        const val CALL_TIMEOUT_SECONDS = 30L

        const val ERROR_NOT_FOUND = "NOT_FOUND"

        const val ERROR_NO_ROUTE = "NO_ROUTE"

        const val ERROR_UNSUPPORTED = "UNSUPPORTED"

        const val ERROR_VENDOR_REFUSED = "VENDOR_REFUSED"

        val ERROR_CODE_KEY: Metadata.Key<String> = Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

        fun authHeaders(config: GameHubConfig): Metadata = Metadata().apply {
            put(Metadata.Key.of("x-operator-id", Metadata.ASCII_STRING_MARSHALLER), config.operatorId)
            put(Metadata.Key.of("x-operator-key", Metadata.ASCII_STRING_MARSHALLER), config.operatorKey)
        }
    }
}
