import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AppConfigTest : FunSpec({

    test("defaults match the historical env contract") {
        val config = AppConfig.fromEnv { null }

        config.grpcPort shouldBe 5050
        config.database.jdbcUrl shouldBe "jdbc:postgresql://localhost:5432/casino"
        config.gameHub.port shouldBe 443
        config.gameHub.plaintext shouldBe false
        config.eventExchange shouldBe "crm.exchange"
        config.freespinToPayout shouldBe true
    }

    test("FREESPIN_TO_PAYOUT is strict: garbage keeps paying rather than quietly stopping") {
        AppConfig.fromEnv { if (it == "FREESPIN_TO_PAYOUT") "nope" else null }.freespinToPayout shouldBe true
        AppConfig.fromEnv { if (it == "FREESPIN_TO_PAYOUT") "false" else null }.freespinToPayout shouldBe false
    }

    test("rabbit credentials are taken verbatim — an @ in an Amazon MQ password needs no escaping") {
        val config = AppConfig.fromEnv { mapOf("RABBIT_PASSWORD" to "p@ss", "RABBIT_TLS" to "true", "RABBIT_PORT" to "5671")[it] }
        config.rabbit shouldBe RabbitConfig("localhost", 5671, "guest", "p@ss", true)
    }
})
