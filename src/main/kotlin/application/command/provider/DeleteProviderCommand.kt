package application.command.provider

import application.ICommand
import domain.vo.Identity

data class DeleteProviderCommand(
    val identity: Identity,
) : ICommand<Unit>
