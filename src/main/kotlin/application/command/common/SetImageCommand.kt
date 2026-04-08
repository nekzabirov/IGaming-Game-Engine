package application.command.common

import application.ICommand
import domain.vo.FileUpload
import domain.vo.Identity

/**
 * Polymorphic command for setting an image on any `Imageable` aggregate.
 *
 * Concrete subclasses (`SetGameImageCommand`, `SetProviderImageCommand`,
 * `SetCollectionImageCommand`) carry the folder name and any aggregate-specific
 * routing metadata. A single `SetImageCommandHandler` dispatches the upload + persist
 * flow without per-entity duplication.
 */
interface SetImageCommand : ICommand<Unit> {

    val identity: Identity

    val key: String

    val file: FileUpload

    val folder: String
}
