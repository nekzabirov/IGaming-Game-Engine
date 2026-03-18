package application.cqrs.freespin

import application.cqrs.IQuery
import domain.vo.Identity

data class GetFreespinPresetsQuery(
    val gameIdentity: Identity,
) : IQuery<Map<String, Any>>