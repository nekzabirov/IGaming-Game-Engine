package application.query.game

import application.IQuery
import domain.model.CasinoGame
import domain.vo.Page
import domain.vo.Pageable

/** RTP bucket relative to [CasinoGame.DEFAULT_RTP]. */
enum class CasinoGameRtpType {
    HOT,
    COLD,
}

/**
 * Paged listing of ACTIVE games bucketed by RTP: [CasinoGameRtpType.HOT] = rtp above the
 * default ordered DESC, [CasinoGameRtpType.COLD] = rtp below the default ordered ASC. A game
 * with no measured rtp (unmeasured, never 0) falls in neither bucket. Catalog position
 * (`order`) is the secondary key (ASC) in both cases.
 */
data class FindAllActiveRtpCasinoGameQuery(
    val type: CasinoGameRtpType,

    val filter: CasinoGameFilter,

    val pageable: Pageable,
) : IQuery<Page<CasinoGame>>
