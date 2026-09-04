package support

import domain.model.Collection
import domain.model.CasinoGame
import domain.model.Platform
import domain.model.PlayerBalance
import domain.model.CasinoProvider
import domain.model.CasinoRound
import domain.model.CasinoSession
import domain.model.Spin
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.ExternalCasinoRoundId
import domain.vo.ExternalSpinId
import domain.vo.FreespinId
import domain.vo.Locale
import domain.vo.PlayerId

/**
 * Reusable builders for domain fixtures in unit tests. Defaults to a minimal valid
 * object graph; callers override only the fields the test cares about.
 */
object TestFixtures {

    fun provider(
        identity: String = "test_provider",
        active: Boolean = true,
    ): CasinoProvider = CasinoProvider(
        identity = Identity(identity),
        name = "Test CasinoProvider",
        active = active,
    )

    fun collection(identity: String = "test_collection"): Collection = Collection(
        identity = Identity(identity),
        name = domain.vo.LocaleName(mapOf("en" to "Test Collection")),
    )

    fun game(
        identity: String = "test_game",
        provider: CasinoProvider = provider(),
        collections: List<Collection> = emptyList(),
        bonusBetEnable: Boolean = true,
        active: Boolean = true,
        locales: List<Locale> = listOf(Locale("en")),
        platforms: List<Platform> = listOf(Platform.DESKTOP, Platform.MOBILE),
    ): CasinoGame = CasinoGame(
        identity = Identity(identity),
        name = "Test CasinoGame",
        provider = provider,
        collections = collections,
        bonusBetEnable = bonusBetEnable,
        active = active,
        freeSpinEnable = true,
        demoEnable = true,
        locales = locales,
        platforms = platforms,
        playLines = 20,
    )

    fun session(
        game: CasinoGame = game(),
        currency: String = "USD",
        locale: String = "en",
        platform: Platform = Platform.DESKTOP,
        playerId: String = "player_1",
    ): CasinoSession = CasinoSession(
        playerId = PlayerId(playerId),
        game = game,
        currency = Currency(currency),
        locale = Locale(locale),
        platform = platform,
    )

    fun round(
        id: Long = 1L,
        game: CasinoGame? = game(),
        playerId: String = "player_1",
        currency: String = "USD",
        externalId: String = "round_1",
        freespinId: String? = null,
    ): CasinoRound = CasinoRound(
        id = id,
        externalId = ExternalCasinoRoundId(externalId),
        freespinId = freespinId?.let { FreespinId(it) },
        playerId = PlayerId(playerId),
        game = game,
        currency = Currency(currency),
    )

    fun spin(
        round: CasinoRound = round(),
        type: SpinType = SpinType.PLACE,
        externalId: String = "spin_1",
        amount: Amount = Amount(100),
        reference: Spin? = null,
    ): Spin = Spin(
        externalId = ExternalSpinId(externalId),
        round = round,
        reference = reference,
        type = type,
        amount = amount,
    )

    fun balance(
        real: Long = 1000,
        bonus: Long = 500,
        currency: String = "USD",
    ): PlayerBalance = PlayerBalance(
        realAmount = Amount(real),
        bonusAmount = Amount(bonus),
        currency = Currency(currency),
    )
}
