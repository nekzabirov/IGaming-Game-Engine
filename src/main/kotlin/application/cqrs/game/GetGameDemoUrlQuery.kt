package application.cqrs.game

import application.cqrs.IQuery
import domain.model.Platform
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.Locale

data class GetGameDemoUrlQuery(
    val identity: Identity,

    val locale: Locale,

    val platform: Platform,

    val currency: Currency,

    val lobbyUrl: String,
) : IQuery<String>
