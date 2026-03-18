package application.cqrs.collection

import application.cqrs.ICommand
import domain.vo.FileUpload
import domain.vo.Identity

data class SetCollectionImageCommand(
    val identity: Identity,

    val key: String,

    val file: FileUpload,
) : ICommand<Unit>
