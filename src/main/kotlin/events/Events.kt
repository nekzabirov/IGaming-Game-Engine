package events

import db.Platform
import db.SpinType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

// The event wire is a PUBLIC contract, older than this engine: crm-engine and consumers we do not
// see here read it. Field names, nesting and formats below are frozen — they are not a reflection
// of the entities, they are what the entities are written INTO before publishing.

/** Shared JSON codec for the envelope and every payload. */
val appJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class ProviderPayload(
    val identity: String,
    val name: String,
    val images: Map<String, String>,
    val customImages: Map<String, String>,
    val order: Int,
    val active: Boolean,
    val blockedCountry: List<String>,
    val tags: List<String>,
    val customTags: List<String>,
)

@Serializable
data class CollectionPayload(
    val identity: String,
    val name: Map<String, String>,
    val tags: List<String>,
    val images: Map<String, String>,
    val active: Boolean,
    val order: Int,
)

@Serializable
data class GamePayload(
    val identity: String,
    val name: String,
    val provider: ProviderPayload,
    val collections: List<CollectionPayload>,
    val bonusBetEnable: Boolean,
    val bonusWageringEnable: Boolean,
    val tags: List<String>,
    val rtp: Double?,
    val freeSpinEnable: Boolean,
    val freeChipEnable: Boolean,
    val jackpotEnable: Boolean,
    val demoEnable: Boolean,
    val bonusBuyEnable: Boolean,
    val locales: List<String>,
    val platforms: List<Platform>,
    val playLines: Int,
    val active: Boolean,
    val images: Map<String, String>,
    val customImages: Map<String, String>,
    val customTags: List<String>,
    val order: Int,
)

/** `game` is null for a sportsbook leg. */
@Serializable
data class RoundPayload(
    val id: Long,
    val externalId: String,
    val freespinId: String?,
    val playerId: String,
    val game: GamePayload?,
    val currency: String,
    val createdAt: Instant,
    val finishedAt: Instant?,
)

@Serializable
data class SpinPayload(
    val id: Long,
    val externalId: String,
    val round: RoundPayload,
    val reference: SpinPayload?,
    val type: SpinType,
    val amount: Long,
    val realAmount: Long,
    val bonusAmount: Long,
)

/** A player opened a game — published once per launch, never persisted. */
@Serializable
data class SessionPayload(
    val playerId: String,
    val game: GamePayload,
    val currency: String,
    val locale: String,
    val platform: Platform,
    val createdAt: Instant,
)

/** Uniform envelope: `{ "playerId": <key>, "data": <payload> }` on a `<domain>.events` route. */
sealed interface AppEvent {
    val route: String

    val playerId: String

    fun data(): JsonElement
}

class SpinEvent(val data: SpinPayload) : AppEvent {
    override val route = ROUTE

    override val playerId = data.round.playerId

    override fun data(): JsonElement = appJson.encodeToJsonElement(SpinWireSerializer, data)

    companion object {
        const val ROUTE = "spin.events"
    }
}

class RoundEvent(val data: RoundPayload) : AppEvent {
    override val route = ROUTE

    override val playerId = data.playerId

    override fun data(): JsonElement = appJson.encodeToJsonElement(RoundWireSerializer, data)

    companion object {
        const val ROUTE = "round.events"
    }
}

class SessionEvent(val data: SessionPayload) : AppEvent {
    override val route = ROUTE

    override val playerId = data.playerId

    override fun data(): JsonElement = appJson.encodeToJsonElement(SessionWireSerializer, data)

    companion object {
        const val ROUTE = "session.events"
    }
}

// Форма провода СОБИРАЕТСЯ, а не отражает модель: раунд уезжает и в плоской форме (`playerId`,
// `game`), и в прежней вложенной (`session.playerId`, `gameVariant.game`) — надмножество, чтобы
// потребители, построенные до ухода сессии и варианта из домена, продолжали читать спины. Сверху —
// плоские `gameIdentity` / `gameProvider` / `currency` / `freespinId`.

/** `gameVariant` не выдаётся без игры: у ноги спортсбука её и не было. */
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

/** A sportsbook leg carries `"game": null` — an explicit null, not a missing key — and has no flat game fields. */
private fun JsonElement.asObjectOrNull(): JsonObject? = if (this is JsonNull) null else jsonObject

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

object SpinWireSerializer : JsonTransformingSerializer<SpinPayload>(SpinPayload.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement {
        val round = element.jsonObject["round"]?.jsonObject

        val withRound = when (round) {
            null -> element.jsonObject
            else -> JsonObject(element.jsonObject + ("round" to round.withLegacyRoundShape()))
        }

        return withRound.withFlatGameFields(
            game = round?.get("game")?.asObjectOrNull(),
            currency = round?.get("currency"),
            freespinId = round?.get("freespinId"),
        )
    }
}

object RoundWireSerializer : JsonTransformingSerializer<RoundPayload>(RoundPayload.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.withLegacyRoundShape().withFlatGameFields(
            game = element.jsonObject["game"]?.asObjectOrNull(),
            currency = element.jsonObject["currency"],
            freespinId = element.jsonObject["freespinId"],
        )
}

object SessionWireSerializer : JsonTransformingSerializer<SessionPayload>(SessionPayload.serializer()) {
    public override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.withFlatGameFields(
            game = element.jsonObject["game"]?.asObjectOrNull(),
            currency = element.jsonObject["currency"],
            freespinId = null,
        )
}
