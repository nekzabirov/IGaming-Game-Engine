package application.query.freespin

import application.IQuery
import domain.vo.Identity

data class GetFreespinPresetsQuery(
    val gameIdentity: Identity,
) : IQuery<Map<String, Any>>