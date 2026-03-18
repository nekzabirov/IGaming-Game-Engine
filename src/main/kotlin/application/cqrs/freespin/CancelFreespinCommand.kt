package application.cqrs.freespin

import application.cqrs.ICommand
import domain.vo.Identity

data class CancelFreespinCommand(
    val gameIdentity: Identity,

    val referenceId: String,
) : ICommand<Unit>
