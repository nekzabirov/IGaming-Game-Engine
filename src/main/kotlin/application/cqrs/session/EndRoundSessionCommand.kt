package application.cqrs.session

import application.cqrs.ICommand

data class EndRoundSessionCommand(
    val sessionToken: String,

    val externalRoundId: String,
) : ICommand<Unit>
