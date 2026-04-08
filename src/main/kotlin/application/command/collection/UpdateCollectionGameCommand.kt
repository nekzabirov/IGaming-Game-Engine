package application.command.collection

import application.ICommand
import domain.vo.Identity

data class UpdateCollectionGameCommand(
    val identity: Identity,

    val addGameIdentities: List<Identity>,
    val deleteGameIdentities: List<Identity>,
) : ICommand<Unit>
