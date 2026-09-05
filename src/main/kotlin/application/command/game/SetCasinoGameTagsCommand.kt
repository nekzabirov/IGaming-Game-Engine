package application.command.game

import application.command.common.SetTagsCommand
import domain.vo.Identity

data class SetCasinoGameTagsCommand(
    override val identity: Identity,

    override val tags: List<String>,
) : SetTagsCommand
