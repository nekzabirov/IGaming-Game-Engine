package application.cqrs.freespin

import application.cqrs.ICommand
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId
import kotlinx.datetime.LocalDateTime

data class CreateFreespinCommand(
    val gameIdentity: Identity,

    val playerId: PlayerId,

    val referenceId: String,

    val currency: Currency,

    val startAt: LocalDateTime,

    val endAt: LocalDateTime,

    val presetValues: Map<String, Any>,
) : ICommand<Unit>
