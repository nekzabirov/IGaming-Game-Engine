package application.port.inbound.command

import application.port.inbound.Command

data class AddGameWonCommand(
    val gameIdentity: String,
    val playerId: String,
    val amount: Long,
    val currency: String
) : Command<Unit>
