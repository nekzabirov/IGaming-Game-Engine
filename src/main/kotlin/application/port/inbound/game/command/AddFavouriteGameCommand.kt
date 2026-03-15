package application.port.inbound.command

import application.port.inbound.Command

data class AddFavouriteGameCommand(
    val gameIdentity: String,
    val playerId: String
) : Command<Unit>
