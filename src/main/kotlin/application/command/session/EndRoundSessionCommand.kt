package application.command.session

import application.ICommand

data class EndRoundSessionCommand(
    val sessionToken: String,

    val externalRoundId: String,
) : ICommand<Unit>
