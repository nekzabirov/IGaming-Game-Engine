package domain.model

import domain.util.Activatable
import domain.util.Imageable
import domain.util.Orderable
import domain.vo.Identity
import domain.vo.ImageMap
import domain.vo.Locale
import kotlinx.serialization.Serializable

@Serializable
data class CasinoGame(
    val identity: Identity,

    val name: String,

    val provider: CasinoProvider,

    val collections: List<Collection> = emptyList(),

    val bonusBetEnable: Boolean = true,

    val bonusWageringEnable: Boolean = true,

    val tags: List<String> = emptyList(),

    /** Null means unmeasured — the hub had no bets to score in its last window — never 0. */
    val rtp: Double? = null,

    val freeSpinEnable: Boolean = false,

    val freeChipEnable: Boolean = false,

    val jackpotEnable: Boolean = false,

    val demoEnable: Boolean = false,

    val bonusBuyEnable: Boolean = false,

    val locales: List<Locale> = emptyList(),

    val platforms: List<Platform> = emptyList(),

    val playLines: Int = 0,

    override var active: Boolean = false,

    override var images: ImageMap = ImageMap.EMPTY,

    /** Local override, keyed the same as [images]. Set only through `SetCasinoGameImageCommand`;
     *  the hub never writes it. Wins per-key when the two are merged for the wire. */
    var customImages: ImageMap = ImageMap.EMPTY,

    override var order: Int = 0,
) : Activatable, Imageable, Orderable {

    fun supportsLocale(locale: Locale): Boolean = locales.contains(locale)

    fun supportsPlatform(platform: Platform): Boolean = platforms.contains(platform)

    /** What the wire actually shows: the hub's own artwork, with our overrides on top. */
    fun resolvedImages(): ImageMap = images.mergedWith(customImages)

    companion object {
        const val DEFAULT_RTP = 96.0
    }
}
