package domain.model

import domain.util.Activatable
import domain.util.Imageable
import domain.util.Orderable
import domain.vo.Identity
import domain.vo.ImageMap

data class Provider(
    val identity: Identity,

    val name: String,

    override var images: ImageMap = ImageMap.EMPTY,

    override var order: Int = 100,

    override var active: Boolean = false,

    val aggregator: Aggregator
) : Activatable, Imageable, Orderable
