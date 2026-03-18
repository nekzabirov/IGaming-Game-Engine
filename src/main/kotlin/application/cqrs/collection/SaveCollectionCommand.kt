package application.cqrs.collection

import application.cqrs.ICommand
import domain.vo.Identity
import domain.vo.LocaleName

data class SaveCollectionCommand(
    val identity: Identity,

    val name: LocaleName,

    val active: Boolean = true,

    val order: Int = 100
) : ICommand<Unit>
