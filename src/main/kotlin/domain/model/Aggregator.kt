package domain.model

import domain.util.Activatable
import domain.vo.Identity

data class Aggregator(
    val identity: Identity,

    val integration: String,

    val config: Map<String, Any>,

    override var active: Boolean,
) : Activatable
