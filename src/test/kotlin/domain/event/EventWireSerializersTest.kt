package domain.event

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class EventWireSerializersTest : FunSpec({

    test("spin wire serializer adds flat game/provider/currency/freespin alongside the nested payload") {
        val nestedSpin = buildJsonObject {
            put("type", JsonPrimitive("PLACE"))
            put("amount", JsonPrimitive(100))
            putJsonObject("round") {
                put("freespinId", JsonPrimitive("fs-123"))
                put("currency", JsonPrimitive("USD"))
                putJsonObject("game") {
                    put("identity", JsonPrimitive("sweet_bonanza"))
                    putJsonObject("provider") {
                        put("identity", JsonPrimitive("pragmatic"))
                    }
                }
            }
        }

        val out = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject

        // flat fields the CRM bridge reads
        out["gameIdentity"] shouldBe JsonPrimitive("sweet_bonanza")
        out["gameProvider"] shouldBe JsonPrimitive("pragmatic")
        out["currency"] shouldBe JsonPrimitive("USD")
        out["freespinId"] shouldBe JsonPrimitive("fs-123")

        // additive — the nested aggregate (used by PlaceSpinEventConsumer) survives untouched
        out["round"]!!.jsonObject["game"]!!.jsonObject["identity"] shouldBe JsonPrimitive("sweet_bonanza")
        out["type"] shouldBe JsonPrimitive("PLACE")
    }

    test("freespinId is omitted when absent") {
        val nestedSpin = buildJsonObject {
            putJsonObject("round") {
                put("currency", JsonPrimitive("EUR"))
                putJsonObject("game") {
                    put("identity", JsonPrimitive("gates_of_olympus"))
                    putJsonObject("provider") { put("identity", JsonPrimitive("pragmatic")) }
                }
            }
        }

        val out = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject

        out.containsKey("freespinId") shouldBe false
        out["gameIdentity"] shouldBe JsonPrimitive("gates_of_olympus")
        out["currency"] shouldBe JsonPrimitive("EUR")
    }

    test("a sportsbook leg (no game) carries no gameIdentity/gameProvider") {
        val nestedSpin = buildJsonObject {
            putJsonObject("round") {
                put("currency", JsonPrimitive("USD"))
            }
        }

        val out = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject

        out.containsKey("gameIdentity") shouldBe false
        out.containsKey("gameProvider") shouldBe false
        out["currency"] shouldBe JsonPrimitive("USD")
    }
})
