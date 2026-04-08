package application.command.freespin

import application.ICommand
import domain.vo.Identity

data class CancelFreespinCommand(
    val gameIdentity: Identity,

    val referenceId: String,
) : ICommand<Unit>
