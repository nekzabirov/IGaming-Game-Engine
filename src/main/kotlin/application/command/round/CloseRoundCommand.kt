package application.command.round

import application.ICommand

data class CloseRoundCommand(val externalRoundId: String) : ICommand<Unit>
