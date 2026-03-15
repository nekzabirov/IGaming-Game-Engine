package application.port.inbound.command

import application.port.inbound.Command

data class RemoveFavouriteGameCommand(
    val gameIdentity: String,
    val playerId: String
) : Command<Unit>
