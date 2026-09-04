package infrastructure.gamehub

import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider
import java.util.concurrent.TimeUnit

/**
 * The single gRPC channel to the hub, shared by every [GameHubClient] call — one address for the
 * whole `GatewayService` surface, the same shape `pamChannel` holds for pam-engine.
 *
 * TLS by default: the hub is reached across a network casino-engine does not own, and a plaintext
 * HTTP/2 preface against its TLS listener comes back as "First received frame was not SETTINGS" —
 * which reads like a protocol bug, not a missing handshake. [GameHubConfig.plaintext] stays
 * available for a local hub over loopback.
 *
 * The TLS provider is pinned to JDK rather than left to pick the default: grpc-netty-shaded prefers
 * its bundled BoringSSL, and loading it in this image kills the JVM outright with a SIGSEGV inside
 * `netty_internal_tcnative_SSLContext_JNI_OnLoad`. The JDK provider is a little slower and does not
 * take the process with it — `main.kt` also sets `io.grpc.netty.shaded.io.netty.handler.ssl.noOpenSsl`
 * before any Netty SSL class loads, which is the belt to this braces.
 */
fun gameHubChannel(config: GameHubConfig): ManagedChannel =
    NettyChannelBuilder
        .forAddress(config.grpcHost, config.grpcPort)
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
