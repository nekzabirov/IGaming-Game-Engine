package application.command.freespin

import application.ICommand
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.PlayerId
import kotlin.time.Duration

/**
 * Проксируется в хаб. Грант называется [referenceId] — ссылкой вызывающего: её хаб отдаёт вендору
 * и ею же называет грант в кошельковых вызовах, так что она возвращается сюда на спин-событиях.
 * Своей записи о гранте casino-engine не держит.
 */
data class CreateFreespinCommand(
    val gameIdentity: Identity,

    val playerId: PlayerId,

    val referenceId: String,

    val currency: Currency,

    val spinAmount: Long,

    val spinCount: Int,

    val duration: Duration,

    val presetValues: Map<String, Any>,
) : ICommand<Unit>
