package application.usecase

import application.port.factory.IAggregatorFactory
import domain.repository.IGameRepository
import domain.repository.IGameVariantRepository
import domain.repository.IProviderRepository
import domain.model.Aggregator
import domain.model.Game
import domain.model.GameVariant
import domain.model.Provider
import domain.vo.GameSymbol
import domain.vo.Identity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SyncAggregatorUsecase(
    private val aggregatorFactory: IAggregatorFactory,
    private val gameRepository: IGameRepository,
    private val gameVariantRepository: IGameVariantRepository,
    private val providerRepository: IProviderRepository
) {

    suspend operator fun invoke(aggregator: Aggregator): Result<Unit> = runCatching {
        process(aggregator)
    }

    private suspend fun process(aggregator: Aggregator) = coroutineScope {
        val gameAdapter = aggregatorFactory.createGameAdapter(aggregator)

        val aggregatorGamesAsync = async { gameAdapter.getAggregatorGames() }
        val gamesAsync = async { gameRepository.findAll() }
        val variantsAsync = async { gameVariantRepository.findAllByIntegration(aggregator.integration) }
        val allProvidersAsync = async { providerRepository.findAll().toMutableList() }

        val updateGames = mutableListOf<Game>()
        val updatedVariants = mutableListOf<GameVariant>()

        for (aggregatorGame in aggregatorGamesAsync.await()) {
            var variant = variantsAsync.await()
                .find { it.symbol.value == aggregatorGame.symbol && it.integration == aggregator.integration }

            if (variant != null) {
                updatedVariants.add(variant.copy(
                    freeSpinEnable = aggregatorGame.freeSpinEnable,
                    freeChipEnable = aggregatorGame.freeChipEnable,
                    jackpotEnable = aggregatorGame.jackpotEnable,
                    demoEnable = aggregatorGame.demoEnable,
                    bonusBuyEnable = aggregatorGame.bonusBuyEnable,
                    platforms = aggregatorGame.platforms,
                    locales = aggregatorGame.locales,
                    playLines = aggregatorGame.playLines,
                ))
                continue
            }

            val providerIdentity = Identity.generate(aggregatorGame.providerName)

            var provider = allProvidersAsync.await().firstOrNull { it.identity == providerIdentity }

            if (provider == null) {
                provider = Provider(
                    identity = providerIdentity,
                    name = aggregatorGame.name,
                    aggregator = aggregator
                ).let { providerRepository.save(it) }

                allProvidersAsync.await().add(provider)
            }

            val gameIdentity = Identity.generate("${providerIdentity}_${aggregatorGame.name}")

            var game = gamesAsync.await().find { it.identity == gameIdentity }

            if (game == null) {
                game = Game(
                    identity = gameIdentity,
                    name = aggregatorGame.name,
                    provider = provider,
                )

                updateGames.add(game)
            }

            variant = GameVariant(
                symbol = GameSymbol(aggregatorGame.symbol),
                name = aggregatorGame.name,
                integration = aggregator.integration,
                game = game,
                providerName = aggregatorGame.providerName,
                freeSpinEnable = aggregatorGame.freeSpinEnable,
                freeChipEnable = aggregatorGame.freeChipEnable,
                jackpotEnable = aggregatorGame.jackpotEnable,
                demoEnable = aggregatorGame.demoEnable,
                bonusBuyEnable = aggregatorGame.bonusBuyEnable,
                locales = aggregatorGame.locales,
                platforms = aggregatorGame.platforms,
                playLines = aggregatorGame.playLines,
            )

            updatedVariants.add(variant)
        }

        gameRepository.saveAll(updateGames)
        gameVariantRepository.saveAll(updatedVariants)
    }

}