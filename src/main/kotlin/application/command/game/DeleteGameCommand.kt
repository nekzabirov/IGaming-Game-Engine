package application.command.game

import application.ICommand
import domain.vo.Identity

data class DeleteGameCommand(
    val identity: Identity,
) : ICommand<Unit>
