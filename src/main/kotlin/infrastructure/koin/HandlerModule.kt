package infrastructure.koin

import infrastructure.handler.aggregator.BatchAggregatorQueryHandler
import infrastructure.handler.aggregator.DeleteAggregatorCommandHandler
import infrastructure.handler.aggregator.FindAggregatorQueryHandler
import infrastructure.handler.aggregator.FindAllAggregatorQueryHandler
import infrastructure.handler.aggregator.SaveAggregatorCommandHandler
import infrastructure.handler.aggregator.SyncAllActiveAggregatorCommandHandler
import infrastructure.handler.collection.AddCollectionGameCommandHandler
import infrastructure.handler.collection.BatchCollectionQueryHandler
import infrastructure.handler.collection.FindAllCollectionQueryHandler
import infrastructure.handler.collection.FindCollectionQueryHandler
import infrastructure.handler.collection.RemoveCollectionGameCommandHandler
import infrastructure.handler.collection.SaveCollectionCommandHandler
import infrastructure.handler.collection.UpdateCollectionGameOrderCommandHandler
import infrastructure.handler.common.SetImageCommandHandler
import infrastructure.handler.freespin.CancelFreespinCommandHandler
import infrastructure.handler.freespin.CreateFreespinCommandHandler
import infrastructure.handler.game.AddGameFavouriteCommandHandler
import infrastructure.handler.game.BatchGameQueryHandler
import infrastructure.handler.game.FindAllGameCollectionQueryHandler
import infrastructure.handler.game.FindAllGamePlayerFavoriteQueryHandler
import infrastructure.handler.game.FindAllGameQueryHandler
import infrastructure.handler.game.FindGameQueryHandler
import infrastructure.handler.game.GetFreespinPresetsQueryHandler
import infrastructure.handler.game.GetGameDemoUrlQueryHandler
import infrastructure.handler.game.PlayGameCommandHandler
import infrastructure.handler.game.RemoveGameFavouriteCommandHandler
import infrastructure.handler.game.SaveGameCommandHandler
import infrastructure.handler.provider.BatchProviderQueryHandler
import infrastructure.handler.provider.FindAllProviderQueryHandler
import infrastructure.handler.provider.FindProviderQueryHandler
import infrastructure.handler.provider.SaveProviderCommandHandler
import infrastructure.handler.round.FindAllRoundQueryHandler
import infrastructure.handler.round.FindRoundQueryHandler
import infrastructure.handler.session.EndRoundSessionHandler
import infrastructure.handler.session.FindSessionBalanceHandler
import infrastructure.handler.session.PlaceSpinSessionHandler
import infrastructure.handler.session.SettleSpinSessionHandler
import infrastructure.handler.winner.LastWinnerQueryHandler
import org.koin.dsl.module

/**
 * Plain Koin singletons for every CQRS handler. The explicit map in [busModule] is what
 * routes commands/queries to these handlers — no marker interfaces, no reflection.
 *
 * Adding a new handler:
 * 1. Add `single { XHandler(...) }` here
 * 2. Add a single line in `busModule` mapping the command/query class to it
 */
val handlerModule = module {
    // Session
    single { PlaceSpinSessionHandler(sessionRepository = get(), roundRepository = get(), processSpinUsecase = get()) }
    single { SettleSpinSessionHandler(sessionRepository = get(), roundRepository = get(), processSpinUsecase = get()) }
    single { EndRoundSessionHandler(sessionRepository = get(), roundRepository = get(), finishRoundUsecase = get()) }
    single { FindSessionBalanceHandler(walletAdapter = get(), sessionRepository = get()) }

    // Game
    single { PlayGameCommandHandler(gameVariantRepository = get(), playerLimitPort = get(), openSessionUsecase = get()) }
    single { SaveGameCommandHandler(gameRepository = get(), providerRepository = get()) }
    single { FindGameQueryHandler() }
    single { FindAllGameQueryHandler() }
    single { BatchGameQueryHandler() }
    single { GetGameDemoUrlQueryHandler(gameVariantRepository = get(), aggregatorFactory = get()) }
    single { FindAllGamePlayerFavoriteQueryHandler() }
    single { FindAllGameCollectionQueryHandler() }
    single { AddGameFavouriteCommandHandler() }
    single { RemoveGameFavouriteCommandHandler() }

    // Freespin
    single { GetFreespinPresetsQueryHandler(aggregatorFactory = get()) }
    single { CreateFreespinCommandHandler(gameVariantRepository = get(), aggregatorFactory = get()) }
    single { CancelFreespinCommandHandler(gameVariantRepository = get(), aggregatorFactory = get()) }

    // Provider
    single { SaveProviderCommandHandler(providerRepository = get(), aggregatorRepository = get()) }
    single { FindProviderQueryHandler() }
    single { FindAllProviderQueryHandler() }
    single { BatchProviderQueryHandler() }

    // Collection
    single { SaveCollectionCommandHandler(collectionRepository = get()) }
    single { FindCollectionQueryHandler() }
    single { FindAllCollectionQueryHandler() }
    single { BatchCollectionQueryHandler() }
    single { AddCollectionGameCommandHandler(collectionRepository = get()) }
    single { RemoveCollectionGameCommandHandler(collectionRepository = get()) }
    single { UpdateCollectionGameOrderCommandHandler(collectionRepository = get()) }

    // Common (polymorphic — serves SetGameImageCommand / SetProviderImageCommand / SetCollectionImageCommand)
    single {
        SetImageCommandHandler(
            fileAdapter = get(),
            gameRepository = get(),
            providerRepository = get(),
            collectionRepository = get(),
        )
    }

    // Aggregator
    single { SaveAggregatorCommandHandler(aggregatorRepository = get()) }
    single { DeleteAggregatorCommandHandler(aggregatorRepository = get()) }
    single { FindAggregatorQueryHandler() }
    single { FindAllAggregatorQueryHandler() }
    single { BatchAggregatorQueryHandler(aggregatorRepository = get()) }
    single { SyncAllActiveAggregatorCommandHandler(get(), get()) }

    // Round
    single { FindRoundQueryHandler() }
    single { FindAllRoundQueryHandler() }

    // Winner
    single { LastWinnerQueryHandler() }
}
