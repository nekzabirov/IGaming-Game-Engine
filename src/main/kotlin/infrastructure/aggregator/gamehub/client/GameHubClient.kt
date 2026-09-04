package infrastructure.aggregator.gamehub.client

import domain.exception.conflict.DemoNotSupportedException
import domain.exception.conflict.FreespinNotSupportedException
import domain.exception.notfound.CasinoGameNotFoundException
import domain.exception.notfound.FreespinNotFoundException
import domain.exception.system.AggregatorUnavailableException
import domain.model.Platform
import gamehub.v1.Common
import gamehub.v1.GameHubServiceGrpc
import gamehub.v1.Gamehub
import infrastructure.aggregator.gamehub.GameHubAdapterProvider
import infrastructure.aggregator.gamehub.GameHubConfig
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.StatusRuntimeException
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider
import io.grpc.stub.MetadataUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Operator → hub gRPC client for `GameHubService`.
 *
 * Every call carries the `x-operator-id` / `x-operator-key` metadata pair, and every failure comes
 * back with the machine-readable reason in the `x-error-code` trailer — the status alone is not
 * specific enough (three different codes share `FAILED_PRECONDITION`).
 */
class GameHubClient(private val config: GameHubConfig) {

    private val stub: GameHubServiceGrpc.GameHubServiceBlockingStub =
        GameHubServiceGrpc.newBlockingStub(channel(config))
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(authHeaders(config)))

    suspend fun gameList(): List<Gamehub.Game> =
        call { it.gameList(Common.Empty.getDefaultInstance()) }.gamesList

    /**
     * [sessionRef] is our own session token. The hub echoes it verbatim on every wallet callback of
     * this session, which is what lets an inbound Spin resolve the exact session row instead of
     * guessing it from (player, game) — a guess that breaks the moment a player opens two tabs on
     * the same game.
     */
    suspend fun openGame(
        game: String,
        playerId: String,
        currency: String,
        lang: String,
        platform: Platform,
        sessionRef: String,
    ): String {
        val request = Gamehub.OpenGameRequest.newBuilder()
            .setGame(game)
            .setPlayerId(playerId)
            .setCurrency(currency)
            .setLang(lang)
            .setPlatform(platform.toProto())
            .setSessionRef(sessionRef)
            .build()

        return call { it.openGame(request) }.url
    }

    suspend fun openGameDemo(
        game: String,
        currency: String,
        lang: String,
        platform: Platform,
    ): String {
        val request = Gamehub.OpenGameDemoRequest.newBuilder()
            .setGame(game)
            .setCurrency(currency)
            .setLang(lang)
            .setPlatform(platform.toProto())
            .build()

        return call { it.openGameDemo(request) }.url
    }

    /** [amount] is the stake per free round in nano — the hub speaks the same unit as the wallet,
     *  so it travels through unscaled. */
    suspend fun createFreespin(
        game: String,
        amount: Long,
        count: Int,
        presets: Map<String, String>,
        playerId: String,
        currency: String,
        externalId: String,
    ) {
        val request = Gamehub.CreateFreespinRequest.newBuilder()
            .setGame(game)
            .setAmount(amount)
            .setCount(count)
            .putAllPresets(presets)
            .setPlayerId(playerId)
            .setCurrency(currency)
            .setExternalId(externalId)
            .build()

        call { it.createFreespin(request) }
    }

    suspend fun cancelFreespin(externalId: String) {
        val request = Gamehub.CancelFreespinRequest.newBuilder()
            .setExternalId(externalId)
            .build()

        call { it.cancelFreespin(request) }
    }

    private suspend fun <T> call(block: (GameHubServiceGrpc.GameHubServiceBlockingStub) -> T): T =
        try {
            withContext(Dispatchers.IO) {
                block(stub.withDeadlineAfter(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            }
        } catch (e: StatusRuntimeException) {
            throw translate(e)
        }

    /**
     * The gRPC status is deliberately coarse — `FAILED_PRECONDITION` alone covers a disabled game,
     * a missing route and an unsupported demo — so the trailer is the API and the status is not.
     * A code with no casino-engine equivalent is rethrown untouched rather than flattened into a
     * wrong domain exception.
     */
    private fun translate(e: StatusRuntimeException): Throwable =
        when (e.trailers?.get(ERROR_CODE_KEY)) {
            ERROR_GAME_NOT_FOUND -> CasinoGameNotFoundException()
            ERROR_FREESPIN_NOT_FOUND -> FreespinNotFoundException()
            ERROR_DEMO_NOT_SUPPORTED -> DemoNotSupportedException()
            ERROR_FREESPIN_NOT_SUPPORTED -> FreespinNotSupportedException()
            ERROR_AGGREGATOR_UNAVAILABLE -> AggregatorUnavailableException(GameHubAdapterProvider.INTEGRATION)
            else -> e
        }

    /** The hub knows two platforms; a download client is served the desktop build. */
    private fun Platform.toProto(): Common.Platform =
        if (this == Platform.MOBILE) Common.Platform.PLATFORM_MOBILE else Common.Platform.PLATFORM_DESKTOP

    private companion object {
        const val CALL_TIMEOUT_SECONDS = 30L

        const val ERROR_GAME_NOT_FOUND = "GAME_NOT_FOUND"

        const val ERROR_FREESPIN_NOT_FOUND = "FREESPIN_NOT_FOUND"

        const val ERROR_DEMO_NOT_SUPPORTED = "DEMO_NOT_SUPPORTED"

        const val ERROR_FREESPIN_NOT_SUPPORTED = "FREESPIN_NOT_SUPPORTED"

        const val ERROR_AGGREGATOR_UNAVAILABLE = "AGGREGATOR_UNAVAILABLE"

        val ERROR_CODE_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER)

        /**
         * `AggregatorRegistry` builds a fresh adapter — and therefore a fresh client — for every
         * request, so a channel owned per instance would open one HTTP/2 connection per launch and
         * never close it. Channels are keyed by address and shared for the life of the process,
         * the way `pamChannel` holds the single channel to pam-engine.
         */
        private val channels = ConcurrentHashMap<String, ManagedChannel>()

        fun channel(config: GameHubConfig): ManagedChannel {
            check(config.grpcHost.isNotBlank()) { "GameHub gRPC host not configured" }

            return channels.computeIfAbsent("${config.grpcHost}:${config.grpcPort}") {
                NettyChannelBuilder
                    .forAddress(config.grpcHost, config.grpcPort)
                    // The hub is reached across a network we do not own, so the channel is TLS
                    // unless the config says otherwise — a plaintext HTTP/2 preface against the
                    // hub's TLS listener comes back as "First received frame was not SETTINGS",
                    // which reads like a protocol bug rather than a missing handshake.
                    // `plaintext: "true"` stays available for a local hub over loopback.
                    //
                    // The TLS provider is pinned to JDK rather than left to pick the default:
                    // grpc-netty-shaded prefers its bundled BoringSSL, and loading it in this
                    // image killed the JVM outright with a SIGSEGV inside
                    // netty_internal_tcnative_SSLContext_JNI_OnLoad. The JDK provider is a little
                    // slower and does not take the process with it.
                    .apply {
                        if (config.plaintext) {
                            usePlaintext()
                        } else {
                            sslContext(GrpcSslContexts.forClient().sslProvider(SslProvider.JDK).build())
                        }
                    }
                    .keepAliveTime(30, TimeUnit.SECONDS)
                    .keepAliveTimeout(10, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .build()
            }
        }

        fun authHeaders(config: GameHubConfig): Metadata = Metadata().apply {
            put(Metadata.Key.of("x-operator-id", Metadata.ASCII_STRING_MARSHALLER), config.operatorId)
            put(Metadata.Key.of("x-operator-key", Metadata.ASCII_STRING_MARSHALLER), config.operatorKey)
        }
    }
}
