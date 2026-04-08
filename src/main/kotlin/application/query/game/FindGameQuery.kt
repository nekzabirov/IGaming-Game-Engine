package application.query.game

import application.IQuery
import domain.model.Game
import domain.model.GameVariant
import domain.vo.Identity
import java.util.Optional

/**
 * Read-side projection of a [Game] together with its currently-active [GameVariant]
 * (if any). Reused by every game-listing query (`FindGameQuery`, `FindAllGameQuery`,
 * `BatchGameQuery`, `FindAllGamePlayerFavoriteQuery`). The variant is optional because
 * a game without an active variant is still a valid catalog row.
 */
data class GameView(
    val game: Game,

    val variant: GameVariant?,
)

data class FindGameQuery(
    val identity: Identity,
) : IQuery<Optional<GameView>>
