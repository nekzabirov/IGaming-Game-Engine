package application.command.collection

import application.command.common.SetImageCommand
import domain.vo.FileUpload
import domain.vo.Identity

data class SetCollectionImageCommand(
    override val identity: Identity,

    override val key: String,

    override val file: FileUpload,
) : SetImageCommand {

    override val folder: String get() = FOLDER

    companion object {
        const val FOLDER: String = "casino/collection"
    }
}
