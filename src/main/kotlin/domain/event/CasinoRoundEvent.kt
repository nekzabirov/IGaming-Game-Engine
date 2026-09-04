package domain.event

import domain.model.CasinoRound
import kotlinx.serialization.KSerializer

data class CasinoRoundEvent(override val data: CasinoRound) : AppEvent<CasinoRound> {

    override val playerId = data.playerId.value

    companion object : AppEvent.Meta<CasinoRound> {
        override val route = "round.events"

        override val serializer: KSerializer<CasinoRound> = CasinoRoundWireSerializer

        override fun create(data: CasinoRound): AppEvent<CasinoRound> = CasinoRoundEvent(data)
    }
}
