package application.command.collection

import application.ICommand
import domain.vo.Identity

data class DeleteCollectionCommand(
    val identity: Identity,
) : ICommand<Unit>
