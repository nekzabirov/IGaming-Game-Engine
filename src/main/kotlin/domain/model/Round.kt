package domain.model

import domain.exception.conflict.RoundAlreadyFinishedException
import domain.exception.domainRequire
import domain.util.ext.LocalDateTimeExt
import kotlinx.datetime.LocalDateTime

data class Round(
    val id: Long = Long.MIN_VALUE,

    val externalId: String,

    val freespinId: String? = null,

    val session: Session,

    val gameVariant: GameVariant = session.gameVariant,

    val createdAt: LocalDateTime = LocalDateTimeExt.now(),

    val finishedAt: LocalDateTime? = null,
) {
    val isFinished: Boolean
        get() = finishedAt != null

    fun finish(): Round {
        domainRequire(!isFinished) { RoundAlreadyFinishedException() }
        return copy(finishedAt = LocalDateTimeExt.now())
    }
}
