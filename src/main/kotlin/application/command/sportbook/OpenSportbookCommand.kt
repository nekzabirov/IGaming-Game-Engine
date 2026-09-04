package application.command.sportbook

import application.ICommand
import domain.vo.Currency
import domain.vo.Locale
import domain.vo.PlayerId

/** Thin proxy onto [infrastructure.gamehub.GameHubClient.openSportbook]. `playerId` null means
 *  guest: the line is visible, no bets, no session minted anywhere. */
data class OpenSportbookCommand(
    val playerId: PlayerId?,

    val currency: Currency,

    val locale: Locale,
) : ICommand<SportbookOpenResult>

data class SportbookOpenResult(
    val integration: String,

    val data: Map<String, String>,
)
