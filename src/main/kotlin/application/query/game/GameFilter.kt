package application.query.game

import domain.vo.Identity

data class GameFilter(
    val query: String,

    val provider: Identity?,

    val inTags: List<String>,

    val bonusBetEnable: Boolean?,

    val bonusWageringEnabled: Boolean?,

    val active: Boolean?,

    val freeSpinEnable: Boolean?,

    val freeChipEnable: Boolean?,

    val jackpotEnable: Boolean?,

    val demoEnable: Boolean?,

    val bonusBuyEnable: Boolean?,
)
