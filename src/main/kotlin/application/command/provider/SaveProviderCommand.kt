package application.command.provider

import application.ICommand
import domain.vo.Identity

data class SaveProviderCommand(
    val identity: Identity,

    val name: String,

    val order: Int,

    val active: Boolean,

    val aggregatorIdentity: Identity
) : ICommand<Unit>
