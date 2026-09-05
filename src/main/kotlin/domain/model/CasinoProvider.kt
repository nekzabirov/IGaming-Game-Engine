package domain.model

import domain.util.Activatable
import domain.util.Imageable
import domain.util.Orderable
import domain.vo.Country
import domain.vo.Identity
import domain.vo.ImageMap
import kotlinx.serialization.Serializable

@Serializable
data class CasinoProvider(
    val identity: Identity,

    val name: String,

    override var images: ImageMap = ImageMap.EMPTY,

    /** Local override, keyed the same as [images]. Set only through `SetCasinoProviderImageCommand`;
     *  the hub never writes it. Wins per-key when the two are merged for the wire. */
    var customImages: ImageMap = ImageMap.EMPTY,

    override var order: Int = 100,

    override var active: Boolean = false,

    val blockedCountry: List<Country> = emptyList(),

    val tags: List<String> = emptyList(),

    /** Local editorial tags on top of [tags]. Set only through `SetCasinoProviderTagsCommand`; the
     *  catalog sync never writes it — [tags] is overwritten wholesale on every run. */
    var customTags: List<String> = emptyList(),
) : Activatable, Imageable, Orderable {

    /** What the wire actually shows: the hub's own artwork, with our overrides on top. */
    fun resolvedImages(): ImageMap = images.mergedWith(customImages)

    /** Same contract as [resolvedImages], one level up: the hub's tags plus ours, deduplicated. */
    fun resolvedTags(): List<String> = (tags + customTags).distinct()
}
