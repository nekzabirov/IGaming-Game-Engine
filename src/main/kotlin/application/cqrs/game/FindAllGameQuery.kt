package application.cqrs.game

import application.cqrs.IQuery
import domain.model.Game
import domain.vo.Identity
import domain.vo.Page
import domain.vo.Pageable

data class FindAllGameQuery(
    val query: String,

    val inProviderIdentities: List<Identity>,

    val inCollectionIdentities: List<Identity>,

    val inTags: List<String>,

    val bonusBetEnable: Boolean?,

    val bonusWageringEnabled: Boolean?,

    val active: Boolean?,

    val freeSpinEnable: Boolean?,

    val freeChipEnable: Boolean?,

    val jackpotEnable: Boolean?,

    val demoEnable: Boolean?,

    val bonusBuyEnable: Boolean?,

    val pageable: Pageable,
) : IQuery<Page<Game>>
