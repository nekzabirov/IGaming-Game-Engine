package domain.model

import domain.util.Activatable
import domain.util.Imageable
import domain.vo.Identity
import domain.vo.ImageMap
import domain.vo.LocaleName

data class Collection(
    val identity: Identity,

    val name: LocaleName,

    override var images: ImageMap = ImageMap.EMPTY,

    override var active: Boolean = true,

    val order: Int = 100
) : Activatable, Imageable
