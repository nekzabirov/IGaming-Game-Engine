package application.cqrs.collection

import application.cqrs.ICommand
import domain.vo.Identity

data class UpdateCollectionGameCommand(
    val identity: Identity,

    val addGameIdentities: List<Identity>,
    val deleteGameIdentities: List<Identity>,
) : ICommand<Unit>
