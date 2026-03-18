package domain.service

import domain.model.Round
import domain.model.Session

object RoundFactory {

    fun open(session: Session, externalId: String, freespinId: String?): Round =
        Round(
            externalId = externalId,
            freespinId = freespinId,
            session = session,
            gameVariant = session.gameVariant
        )
}
