package application.cqrs.game

import application.cqrs.ICommand
import domain.vo.Identity

data class SaveGameCommand(
    val identity: Identity,

    val name: String,

    val bonusBetEnable: Boolean,
    val bonusWageringEnable: Boolean,

    val tags: List<String>,

    val providerIdentity: Identity,
) : ICommand<Unit>
