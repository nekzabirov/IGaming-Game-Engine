package application.cqrs.provider

import application.cqrs.ICommand
import domain.vo.FileUpload
import domain.vo.Identity

data class SetProviderImageCommand(
    val identity: Identity,

    val key: String,

    val file: FileUpload,
) : ICommand<Unit>
