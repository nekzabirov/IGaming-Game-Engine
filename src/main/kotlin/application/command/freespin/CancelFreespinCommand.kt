package application.command.freespin

import application.ICommand
import domain.vo.Identity

/** Отмена по ссылке ВЫЗЫВАЮЩЕГО — той же, под которой грант создавался. */
data class CancelFreespinCommand(
    val gameIdentity: Identity,

    val referenceId: String,
) : ICommand<Unit>
