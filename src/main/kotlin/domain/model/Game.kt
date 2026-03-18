package domain.model

import domain.util.Activatable
import domain.util.Imageable
import domain.util.Orderable
import domain.vo.Identity
import domain.vo.ImageMap

data class Game(
    val identity: Identity,

    val name: String,

    val provider: Provider,

    val collections: List<Collection> = emptyList(),

    val bonusBetEnable: Boolean = true,

    val bonusWageringEnable: Boolean = true,

    val tags: List<String> = emptyList(),

    override var active: Boolean = false,

    override var images: ImageMap = ImageMap.EMPTY,

    override var order: Int = 0,
) : Activatable, Imageable, Orderable {

    lateinit var variant: GameVariant

    val hasVariant: Boolean
        get() = ::variant.isInitialized

}
