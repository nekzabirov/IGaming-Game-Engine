package infrastructure.aggregator.onegamehub

class OneGameHubConfig(config: Map<String, Any>) {
    val salt = config["salt"]?.toString() ?: ""

    val secret = config["secret"]?.toString() ?: ""

    val partner = config["partner"]?.toString() ?: ""

    val gateway = config["gateway"]?.toString() ?: ""
}