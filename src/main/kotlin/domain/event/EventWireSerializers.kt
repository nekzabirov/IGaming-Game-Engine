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
 * External consumers (the miniapp CRM bridge) read flat scalar fields, so these serializers add
 * `gameIdentity` / `gameProvider` / `currency` / `freespinId` at the TOP level **alongside** the
 * full nested payload — additive only. Internal consumers (e.g. PlaceSpinEventConsumer) keep
 * decoding the nested aggregate; the extra keys are ignored via `appJson { ignoreUnknownKeys }`.
 * `game` is nullable now (a sportsbook leg carries none) — `gameIdentity`/`gameProvider` are simply
 * absent for those.
 */
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
        return element.jsonObject.withFlatGameFields(
            game = round?.get("game")?.jsonObject,
            currency = round?.get("currency"),
            freespinId = round?.get("freespinId"),
        )
    }
}

object CasinoRoundWireSerializer : JsonTransformingSerializer<CasinoRound>(CasinoRound.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.withFlatGameFields(
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
