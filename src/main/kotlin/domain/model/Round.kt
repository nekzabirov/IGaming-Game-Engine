package domain.model

import domain.event.RoundFinished
import domain.event.WithEvents
import domain.exception.conflict.RoundAlreadyFinishedException
import domain.exception.domainRequire
import domain.util.ext.LocalDateTimeExt
import domain.vo.ExternalRoundId
import domain.vo.FreespinId
import kotlinx.datetime.LocalDateTime

data class Round(
    val id: Long = Long.MIN_VALUE,

    val externalId: ExternalRoundId,

    val freespinId: FreespinId? = null,

    val session: Session,

    val gameVariant: GameVariant = session.gameVariant,

    val createdAt: LocalDateTime = LocalDateTimeExt.now(),

    val finishedAt: LocalDateTime? = null,
) {
    val isFinished: Boolean
        get() = finishedAt != null

    /**
     * Closes the round. Returns the updated [Round] alongside a [RoundFinished] domain
     * event the usecase should publish after persistence commits.
     *
     * Throws [RoundAlreadyFinishedException] if the round was already closed.
     */
    fun finish(): WithEvents<Round> {
        domainRequire(!isFinished) { RoundAlreadyFinishedException() }
        val finished = copy(finishedAt = LocalDateTimeExt.now())
        return WithEvents(finished, listOf(RoundFinished(finished)))
    }
}
