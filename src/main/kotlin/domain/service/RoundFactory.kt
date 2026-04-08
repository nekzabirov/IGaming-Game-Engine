package domain.service

import domain.model.Round
import domain.model.Session
import domain.vo.ExternalRoundId
import domain.vo.FreespinId

object RoundFactory {

    fun open(session: Session, externalId: ExternalRoundId, freespinId: FreespinId?): Round =
        Round(
            externalId = externalId,
            freespinId = freespinId,
            session = session,
            gameVariant = session.gameVariant,
        )
}
