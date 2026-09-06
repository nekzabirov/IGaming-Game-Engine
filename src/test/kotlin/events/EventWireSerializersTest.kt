package events

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
                    putJsonObject("provider") { put("identity", JsonPrimitive("pragmatic")) }
                }
            }
        }

        val out = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject

        out["gameIdentity"] shouldBe JsonPrimitive("sweet_bonanza")
        out["gameProvider"] shouldBe JsonPrimitive("pragmatic")
        out["currency"] shouldBe JsonPrimitive("USD")
        out["freespinId"] shouldBe JsonPrimitive("fs-123")
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
        val nestedSpin = buildJsonObject { putJsonObject("round") { put("currency", JsonPrimitive("USD")) } }

        val out = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject

        out.containsKey("gameIdentity") shouldBe false
        out.containsKey("gameProvider") shouldBe false
        out["currency"] shouldBe JsonPrimitive("USD")
    }

    test("раунд едет и в прежней вложенной форме: session.playerId и gameVariant.game") {
        val nestedSpin = buildJsonObject {
            put("type", JsonPrimitive("SETTLE"))
            putJsonObject("round") {
                put("playerId", JsonPrimitive("1"))
                put("currency", JsonPrimitive("UAH"))
                putJsonObject("game") {
                    put("identity", JsonPrimitive("pragmatic_gates_of_olympus"))
                    putJsonObject("provider") { put("identity", JsonPrimitive("pragmatic")) }
                }
            }
        }

        val round = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject["round"]!!.jsonObject

        round["session"]!!.jsonObject["playerId"] shouldBe JsonPrimitive("1")
        round["gameVariant"]!!.jsonObject["game"]!!.jsonObject["identity"] shouldBe JsonPrimitive("pragmatic_gates_of_olympus")
        round["playerId"] shouldBe JsonPrimitive("1")
        round["game"]!!.jsonObject["identity"] shouldBe JsonPrimitive("pragmatic_gates_of_olympus")
    }

    test("событие раунда несёт ту же пару форм") {
        val round = buildJsonObject {
            put("playerId", JsonPrimitive("42"))
            put("currency", JsonPrimitive("UAH"))
            put("freespinId", JsonPrimitive("1002"))
            putJsonObject("game") {
                put("identity", JsonPrimitive("skyline_slap_club"))
                putJsonObject("provider") { put("identity", JsonPrimitive("skyline")) }
            }
        }

        val out = RoundWireSerializer.transformSerialize(round).jsonObject

        out["session"]!!.jsonObject["playerId"] shouldBe JsonPrimitive("42")
        out["gameVariant"]!!.jsonObject["game"]!!.jsonObject["identity"] shouldBe JsonPrimitive("skyline_slap_club")
        out["freespinId"] shouldBe JsonPrimitive("1002")
    }

    test("без игры вложенного варианта нет") {
        val nestedSpin = buildJsonObject {
            putJsonObject("round") {
                put("playerId", JsonPrimitive("7"))
                put("currency", JsonPrimitive("USD"))
            }
        }

        val round = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject["round"]!!.jsonObject

        round.containsKey("gameVariant") shouldBe false
        round["session"]!!.jsonObject["playerId"] shouldBe JsonPrimitive("7")
    }
})

class EventWireNullGameTest : FunSpec({

    test("an explicit null game (a sportsbook leg as the payload serializes it) is not a crash and gets no flat fields") {
        val nestedSpin = buildJsonObject {
            putJsonObject("round") {
                put("playerId", JsonPrimitive("7"))
                put("game", kotlinx.serialization.json.JsonNull)
                put("currency", JsonPrimitive("USD"))
            }
        }

        val out = SpinWireSerializer.transformSerialize(nestedSpin).jsonObject

        out.containsKey("gameIdentity") shouldBe false
        out["round"]!!.jsonObject.containsKey("gameVariant") shouldBe false
        out["round"]!!.jsonObject["session"]!!.jsonObject["playerId"] shouldBe JsonPrimitive("7")

        val round = buildJsonObject {
            put("playerId", JsonPrimitive("7"))
            put("game", kotlinx.serialization.json.JsonNull)
            put("currency", JsonPrimitive("USD"))
        }
        RoundWireSerializer.transformSerialize(round).jsonObject.containsKey("gameIdentity") shouldBe false
    }
})
