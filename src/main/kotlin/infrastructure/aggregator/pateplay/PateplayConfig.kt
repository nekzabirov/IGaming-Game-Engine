package infrastructure.aggregator.pateplay

class PateplayConfig(config: Map<String, Any>) {

    val gatewayUrl = config["gatewayUrl"]?.toString() ?: ""

    val siteCode = config["siteCode"]?.toString() ?: ""

    val gatewayApiKey = config["gatewayApiKey"]?.toString() ?: ""

    val gatewayApiSecret = config["gatewayApiSecret"]?.toString() ?: ""

    val gameLaunchUrl = config["gameLaunchUrl"]?.toString() ?: ""

    val gameDemoLaunchUrl = config["gameDemoLaunchUrl"]?.toString() ?: ""

    val walletApiKey = config["walletApiKey"]?.toString() ?: ""

    val walletApiSecret = config["walletApiSecret"]?.toString() ?: ""
}
