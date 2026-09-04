package domain.event

import domain.model.Spin
import kotlinx.serialization.KSerializer

data class SpinEvent(override val data: Spin) : AppEvent<Spin> {

    override val playerId = data.round.playerId.value

    companion object : AppEvent.Meta<Spin> {
        override val route = "spin.events"

        override val serializer: KSerializer<Spin> = SpinWireSerializer

        override fun create(data: Spin): AppEvent<Spin> = SpinEvent(data)
    }
}
