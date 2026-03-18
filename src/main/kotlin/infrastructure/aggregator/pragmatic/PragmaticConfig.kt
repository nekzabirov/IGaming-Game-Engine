package infrastructure.aggregator.pragmatic

class PragmaticConfig(config: Map<String, Any>) {

    val secretKey = config["secretKey"]?.toString() ?: ""

    val secureLogin = config["secureLogin"]?.toString() ?: ""

    val gatewayUrl = config["gatewayUrl"]?.toString() ?: ""
}
