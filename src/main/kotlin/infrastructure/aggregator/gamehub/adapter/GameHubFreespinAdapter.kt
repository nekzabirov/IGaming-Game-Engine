package infrastructure.aggregator.gamehub.adapter

import application.port.external.IFreespinPort
import domain.vo.Currency
import domain.vo.PlayerId
import infrastructure.aggregator.gamehub.GameHubConfig
import infrastructure.aggregator.gamehub.client.GameHubClient
import kotlinx.datetime.LocalDateTime

/**
 * Free rounds granted through the hub. The grant is identified end to end by OUR reference id: it
 * travels as `external_id` and comes back as `SpinRequest.freespin_id` on every spin the grant
 * pays for.
 *
 * The validity window is enforced locally — the hub's contract carries no start/end, because the
 * vendors behind it disagree on whether a grant expires at all, and `Freespin.isRedeemableAt` is
 * authoritative either way.
 */
class GameHubFreespinAdapter(
    config: GameHubConfig,
) : IFreespinPort {

    private val client = GameHubClient(config)

    /** The hub exposes no preset API: the per-vendor knobs are whatever the caller already knows,
     *  and it forwards them verbatim. */
    override suspend fun getPreset(gameSymbol: String): Map<String, Any> = emptyMap()

    override suspend fun create(
        presetValue: Map<String, Any>,
        referenceId: String,
        playerId: PlayerId,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime,
        spinAmount: Long,
        spinCount: Int
    ) {
        client.createFreespin(
            game = gameSymbol,
            // Both sides count in nano, so the stake is passed through unscaled.
            amount = spinAmount,
            count = spinCount,
            presets = presetValue.mapValues { (_, value) -> value.toString() },
            playerId = playerId.value,
            currency = currency.value,
            externalId = referenceId,
        )
    }

    override suspend fun cancel(referenceId: String) = client.cancelFreespin(referenceId)
}
