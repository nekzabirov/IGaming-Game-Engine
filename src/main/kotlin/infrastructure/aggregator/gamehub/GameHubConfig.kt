package infrastructure.aggregator.gamehub

/**
 * GameHub integration config.
 *
 * GameHub is our own hub: it sits in front of several vendor aggregators, normalises them into one
 * gRPC contract, and calls back into the operator's wallet. From casino-engine's side it is a
 * single gRPC endpoint authenticated by an operator identity and a shared secret — the pair is sent
 * as the `x-operator-id` / `x-operator-key` metadata headers on every outbound call.
 */
class GameHubConfig(config: Map<String, Any>) {

    /** Hub gRPC host, e.g. `gamehub.prematch.internal`. No scheme — gRPC dials host:port. */
    val grpcHost = config["grpcHost"]?.toString()?.trim() ?: ""

    /** The aggregator config map is JSON-decoded, so a numeric port arrives as a Number and its
     *  `toString()` is `9090.0` — parsing it as an Int directly would silently fall back. */
    val grpcPort = (config["grpcPort"] as? Number)?.toInt()
        ?: config["grpcPort"]?.toString()?.trim()?.toDoubleOrNull()?.toInt()
        ?: DEFAULT_GRPC_PORT

    /** Our identity at the hub — sent as `x-operator-id`. */
    val operatorId = config["operatorId"]?.toString() ?: ""

    /** The shared secret — sent as `x-operator-key`, compared in constant time by the hub. */
    val operatorKey = config["operatorKey"]?.toString() ?: ""

    private companion object {
        const val DEFAULT_GRPC_PORT = 9090
    }
}
