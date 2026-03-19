package infrastructure.koin

import application.cqrs.ICommandHandler
import application.cqrs.IQueryHandler
import application.cqrs.game.AddGameFavouriteCommand
import application.cqrs.game.RemoveGameFavouriteCommand
import infrastructure.handler.aggregator.FindAggregatorQueryHandler
import infrastructure.handler.aggregator.FindAllAggregatorQueryHandler
import infrastructure.handler.aggregator.SaveAggregatorCommandHandler
import infrastructure.handler.aggregator.SyncAllActiveAggregatorCommandHandler
import infrastructure.handler.collection.BatchCollectionQueryHandler
import infrastructure.handler.collection.FindAllCollectionQueryHandler
import infrastructure.handler.collection.FindCollectionQueryHandler
import infrastructure.handler.collection.SaveCollectionCommandHandler
import infrastructure.handler.collection.SetCollectionImageCommandHandler
import infrastructure.handler.collection.UpdateCollectionGameCommandHandler
import infrastructure.handler.freespin.CancelFreespinCommandHandler
import infrastructure.handler.freespin.CreateFreespinCommandHandler
import infrastructure.handler.game.AddGameFavouriteCommandHandler
import infrastructure.handler.game.BatchGameQueryHandler
import infrastructure.handler.game.FindAllGamePlayerFavoriteQueryHandler
import infrastructure.handler.game.FindAllGameQueryHandler
import infrastructure.handler.game.FindGameQueryHandler
import infrastructure.handler.game.GetFreespinPresetsQueryHandler
import infrastructure.handler.game.GetGameDemoUrlQueryHandler
import infrastructure.handler.game.PlayGameCommandHandler
import infrastructure.handler.game.RemoveGameFavouriteCommandHandler
import infrastructure.handler.game.SaveGameCommandHandler
import infrastructure.handler.game.SetGameImageCommandHandler
import infrastructure.handler.provider.BatchProviderQueryHandler
import infrastructure.handler.provider.FindAllProviderQueryHandler
import infrastructure.handler.provider.FindProviderQueryHandler
import infrastructure.handler.provider.SaveProviderCommandHandler
import infrastructure.handler.provider.SetProviderImageCommandHandler
import infrastructure.handler.round.FindAllRoundQueryHandler
import infrastructure.handler.round.FindRoundQueryHandler
import infrastructure.handler.session.EndRoundSessionHandler
import infrastructure.handler.session.FindSessionBalanceHandler
import infrastructure.handler.session.PlaceSpinSessionHandler
import infrastructure.handler.session.SettleSpinSessionHandler
import infrastructure.handler.winner.LastWinnerQueryHandler
import org.koin.dsl.module

val handlerModule = module {
    // Session
    single { PlaceSpinSessionHandler(sessionRepository = get(), roundRepository = get(), processSpinUsecase = get()) }
    single { SettleSpinSessionHandler(sessionRepository = get(), roundRepository = get(), processSpinUsecase = get()) }
    single { EndRoundSessionHandler(sessionRepository = get(), roundRepository = get(), finishRoundUsecase = get()) }
    single { FindSessionBalanceHandler(walletAdapter = get(), sessionRepository = get()) }

    // Game
    single { PlayGameCommandHandler(gameVariantRepository = get(), playerLimitPort = get(), openSessionUsecase = get()) }
    single { SaveGameCommandHandler() }
    single { FindGameQueryHandler() }
    single { FindAllGameQueryHandler() }
    single { BatchGameQueryHandler() }
    single { SetGameImageCommandHandler(fileAdapter = get()) }
    single { GetGameDemoUrlQueryHandler(gameVariantRepository = get(), aggregatoryFactory = get()) }
    single { FindAllGamePlayerFavoriteQueryHandler() }
    single { AddGameFavouriteCommandHandler() }
    single { RemoveGameFavouriteCommandHandler() }

    // Freespin
    single { GetFreespinPresetsQueryHandler(aggregatoryFactory = get()) }
    single { CreateFreespinCommandHandler(gameVariantRepository = get(), aggregatoryFactory = get()) }
    single { CancelFreespinCommandHandler(gameVariantRepository = get(), aggregatoryFactory = get()) }

    // Provider
    single { SaveProviderCommandHandler() }
    single { FindProviderQueryHandler() }
    single { FindAllProviderQueryHandler() }
    single { BatchProviderQueryHandler() }
    single { SetProviderImageCommandHandler(fileAdapter = get()) }

    // Collection
    single { SaveCollectionCommandHandler() }
    single { FindCollectionQueryHandler() }
    single { FindAllCollectionQueryHandler() }
    single { BatchCollectionQueryHandler() }
    single { SetCollectionImageCommandHandler(fileAdapter = get()) }
    single { UpdateCollectionGameCommandHandler() }

    // Aggregator
    single { SaveAggregatorCommandHandler() }
    single { FindAggregatorQueryHandler() }
    single { FindAllAggregatorQueryHandler() }
    single { SyncAllActiveAggregatorCommandHandler(get(), get()) }

    // Round
    single { FindRoundQueryHandler() }
    single { FindAllRoundQueryHandler() }

    // Winner
    single { LastWinnerQueryHandler() }
}
