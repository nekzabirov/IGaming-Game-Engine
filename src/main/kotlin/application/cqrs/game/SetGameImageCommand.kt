package application.cqrs.game

import application.cqrs.ICommand
import domain.vo.FileUpload
import domain.vo.Identity

data class SetGameImageCommand(
    val identity: Identity,

    val key: String,

    val file: FileUpload,
) : ICommand<Unit>
