package support

import domain.model.Aggregator
import domain.model.Collection
import domain.model.Game
import domain.model.GameVariant
import domain.model.Platform
import domain.model.PlayerBalance
import domain.model.Provider
import domain.model.Round
import domain.model.Session
import domain.model.Spin
import domain.model.SpinType
import domain.vo.Amount
import domain.vo.Currency
import domain.vo.Identity
import domain.vo.ExternalRoundId
import domain.vo.ExternalSpinId
import domain.vo.FreespinId
import domain.vo.GameSymbol
import domain.vo.Locale
import domain.vo.PlayerId
import domain.vo.SessionToken

/**
 * Reusable builders for domain fixtures in unit tests. Defaults to a minimal valid
 * object graph; callers override only the fields the test cares about.
 */
object TestFixtures {

    fun aggregator(
        identity: String = "test_agg",
        integration: String = "ONEGAMEHUB",
        active: Boolean = true,
        config: Map<String, Any> = emptyMap(),
    ): Aggregator = Aggregator(
        identity = Identity(identity),
        integration = integration,
        config = config,
        active = active,
    )

    fun provider(
        identity: String = "test_provider",
        aggregator: Aggregator = aggregator(),
        active: Boolean = true,
    ): Provider = Provider(
        identity = Identity(identity),
        name = "Test Provider",
        aggregator = aggregator,
        active = active,
    )

    fun collection(identity: String = "test_collection"): Collection = Collection(
        identity = Identity(identity),
        name = domain.vo.LocaleName(mapOf("en" to "Test Collection")),
    )

    fun game(
        identity: String = "test_game",
        provider: Provider = provider(),
        collections: List<Collection> = emptyList(),
        bonusBetEnable: Boolean = true,
        active: Boolean = true,
    ): Game = Game(
        identity = Identity(identity),
        name = "Test Game",
        provider = provider,
        collections = collections,
        bonusBetEnable = bonusBetEnable,
        active = active,
    )

    fun gameVariant(
        game: Game = game(),
        symbol: String = "tg_01",
        locales: List<Locale> = listOf(Locale("en")),
        platforms: List<Platform> = listOf(Platform.DESKTOP, Platform.MOBILE),
    ): GameVariant = GameVariant(
        id = 1L,
        symbol = GameSymbol(symbol),
        name = game.name,
        integration = game.provider.aggregator.integration,
        game = game,
        providerName = game.provider.name,
        freeSpinEnable = true,
        freeChipEnable = false,
        jackpotEnable = false,
        demoEnable = true,
        bonusBuyEnable = false,
        locales = locales,
        platforms = platforms,
        playLines = 20,
    )

    fun session(
        id: Long = 1L,
        variant: GameVariant = gameVariant(),
        currency: String = "USD",
        locale: String = "en",
        platform: Platform = Platform.DESKTOP,
        playerId: String = "player_1",
        token: String = "token_abc",
    ): Session = Session(
        id = id,
        gameVariant = variant,
        playerId = PlayerId(playerId),
        token = SessionToken(token),
        externalToken = null,
        currency = Currency(currency),
        locale = Locale(locale),
        platform = platform,
    )

    fun round(
        id: Long = 1L,
        session: Session = session(),
        externalId: String = "round_1",
        freespinId: String? = null,
    ): Round = Round(
        id = id,
        externalId = ExternalRoundId(externalId),
        freespinId = freespinId?.let { FreespinId(it) },
        session = session,
    )

    fun spin(
        round: Round = round(),
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
