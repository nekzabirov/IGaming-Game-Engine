package application.command.provider

import application.ICommand
import domain.vo.Country
import domain.vo.Identity

/** Update-only — `name`/`images`/`tags` are GameHub's, written only by the catalog sync. Fails
 *  with [domain.exception.notfound.CasinoProviderNotFoundException] unless sync already created
 *  the provider. */
data class SaveCasinoProviderCommand(
    val identity: Identity,

    val order: Int,

    val active: Boolean,

    val blockedCountry: List<Country> = emptyList(),
) : ICommand<Unit>
