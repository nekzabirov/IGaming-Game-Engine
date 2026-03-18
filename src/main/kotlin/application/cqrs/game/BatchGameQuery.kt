package application.cqrs.game

import application.cqrs.IQuery
import domain.model.Game

class BatchGameQuery : IQuery<List<Game>>
