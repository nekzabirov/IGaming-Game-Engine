package application.cqrs.game

import application.cqrs.ICommand
import domain.vo.Identity
import domain.vo.PlayerId

data class AddGameFavouriteCommand(val identity: Identity, val playerId: PlayerId) : ICommand<Unit>

data class RemoveGameFavouriteCommand(val identity: Identity, val playerId: PlayerId) : ICommand<Unit>