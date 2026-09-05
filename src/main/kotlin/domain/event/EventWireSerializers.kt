package domain.event

import domain.model.CasinoRound
import domain.model.CasinoSession
import domain.model.Spin
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

/**
 * Форма события — ПУБЛИЧНЫЙ контракт, и она старше этого движка: на неё построены потребители,
 * которых здесь не видно. Убрав из домена `CasinoSession` и `CasinoGameVariant`, мы вынесли их и с
 * провода — и каждый спин перестал разбираться у читателей, молча, без единой ошибки на нашей
 * стороне.
 *
 * Поэтому провод здесь СОБИРАЕТСЯ, а не отражает домен: раунд уезжает и в новой плоской форме
 * (`playerId`, `game`), и в прежней вложенной (`session.playerId`, `gameVariant.game`). Надмножество
 * — единственный вид совместимости, который не требует одновременного релиза на той стороне.
 *
 * Сверху добавляются ещё и плоские `gameIdentity` / `gameProvider` / `currency` / `freespinId`.
 * Внутренние потребители продолжают читать вложенный агрегат; лишние ключи гасит
 * `appJson { ignoreUnknownKeys }`.
 */
/**
 * Достраивает раунду прежнюю вложенную форму.
 *
 * `gameVariant` не выдаётся, когда игры нет: у ноги спортсбука её и не было, а пустая обёртка
 * соврала бы читателю про игру, которой не существует.
 */
private fun JsonObject.withLegacyRoundShape(): JsonObject {
    val round = this
    val legacy = buildMap<String, JsonElement> {
        round["playerId"]?.takeIf { it !is JsonNull }?.let {
            put("session", JsonObject(mapOf("playerId" to it)))
        }
        round["game"]?.takeIf { it !is JsonNull }?.let {
            put("gameVariant", JsonObject(mapOf("game" to it)))
        }
    }

    return JsonObject(round + legacy)
}

private fun JsonObject.withFlatGameFields(
    game: JsonObject?,
    currency: JsonElement?,
    freespinId: JsonElement?,
): JsonObject {
    val extras = buildMap<String, JsonElement> {
        game?.get("identity")?.let { put("gameIdentity", it) }
        game?.get("provider")?.jsonObject?.get("identity")?.let { put("gameProvider", it) }
        currency?.takeIf { it !is JsonNull }?.let { put("currency", it) }
        freespinId?.takeIf { it !is JsonNull }?.let { put("freespinId", it) }
    }
    return JsonObject(this + extras)
}

object SpinWireSerializer : JsonTransformingSerializer<Spin>(Spin.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement {
        val round = element.jsonObject["round"]?.jsonObject

        val withRound = when (round) {
            null -> element.jsonObject
            else -> JsonObject(element.jsonObject + ("round" to round.withLegacyRoundShape()))
        }

        return withRound.withFlatGameFields(
            game = round?.get("game")?.jsonObject,
            currency = round?.get("currency"),
            freespinId = round?.get("freespinId"),
        )
    }
}

object CasinoRoundWireSerializer : JsonTransformingSerializer<CasinoRound>(CasinoRound.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.withLegacyRoundShape().withFlatGameFields(
            game = element.jsonObject["game"]?.jsonObject,
            currency = element.jsonObject["currency"],
            freespinId = element.jsonObject["freespinId"],
        )
}

object CasinoSessionWireSerializer : JsonTransformingSerializer<CasinoSession>(CasinoSession.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.withFlatGameFields(
            game = element.jsonObject["game"]?.jsonObject,
            currency = element.jsonObject["currency"],
            freespinId = null,
        )
}
