package application.command.provider

import application.command.common.SetTagsCommand
import domain.vo.Identity

data class SetCasinoProviderTagsCommand(
    override val identity: Identity,

    override val tags: List<String>,
) : SetTagsCommand
