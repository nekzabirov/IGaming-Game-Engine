package infrastructure.koin

import infrastructure.handler.collection.AddCollectionCasinoGameCommandHandler
import infrastructure.handler.collection.BatchCollectionQueryHandler
import infrastructure.handler.collection.DeleteCollectionCommandHandler
import infrastructure.handler.collection.FindAllCollectionQueryHandler
import infrastructure.handler.collection.FindCollectionQueryHandler
import infrastructure.handler.collection.RemoveCollectionCasinoGameCommandHandler
import infrastructure.handler.collection.SaveCollectionCommandHandler
import infrastructure.handler.collection.UpdateCollectionCasinoGameOrderCommandHandler
import infrastructure.handler.common.SetImageCommandHandler
import infrastructure.handler.freespin.CancelFreespinCommandHandler
import infrastructure.handler.freespin.CreateFreespinCommandHandler
import infrastructure.handler.freespin.GetFreespinPresetsQueryHandler
import infrastructure.handler.game.AddCasinoGameFavouriteCommandHandler
import infrastructure.handler.game.BatchCasinoGameQueryHandler
import infrastructure.handler.game.FindAllActiveRtpCasinoGameQueryHandler
import infrastructure.handler.game.FindAllCasinoGameCollectionQueryHandler
import infrastructure.handler.game.FindAllCasinoGamePlayerFavoriteQueryHandler
import infrastructure.handler.game.FindAllCasinoGamePlayerLastQueryHandler
import infrastructure.handler.game.FindAllCasinoGameQueryHandler
import infrastructure.handler.game.FindAllCasinoGameTagQueryHandler
import infrastructure.handler.game.FindCasinoGameQueryHandler
import infrastructure.handler.game.GetCasinoGameDemoUrlQueryHandler
import infrastructure.handler.game.PlayCasinoGameCommandHandler
import infrastructure.handler.game.RemoveCasinoGameFavouriteCommandHandler
import infrastructure.handler.game.SaveCasinoGameCommandHandler
import infrastructure.handler.provider.BatchCasinoProviderQueryHandler
import infrastructure.handler.provider.FindAllCasinoProviderQueryHandler
import infrastructure.handler.provider.FindAllCasinoProviderTagQueryHandler
import infrastructure.handler.provider.FindCasinoProviderQueryHandler
import infrastructure.handler.provider.SaveCasinoProviderCommandHandler
import infrastructure.handler.round.CloseRoundHandler
import infrastructure.handler.round.FindAllCasinoRoundQueryHandler
import infrastructure.handler.round.FindBalanceHandler
import infrastructure.handler.round.FindCasinoRoundQueryHandler
import infrastructure.handler.round.PlaceSpinHandler
import infrastructure.handler.round.ReopenRoundHandler
import infrastructure.handler.round.RollbackSpinHandler
import infrastructure.handler.round.SettleSpinHandler
import infrastructure.handler.sportbook.OpenSportbookHandler
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
    // CasinoRound / wallet
    single { PlaceSpinHandler(gameRepository = get(), roundRepository = get(), spinRepository = get(), processSpinUsecase = get(), walletPort = get()) }
    single { SettleSpinHandler(gameRepository = get(), roundRepository = get(), spinRepository = get(), processSpinUsecase = get(), walletPort = get()) }
    single { RollbackSpinHandler(spinRepository = get(), processSpinUsecase = get(), walletPort = get()) }
    single { CloseRoundHandler(roundRepository = get(), finishRoundUsecase = get()) }
    single { ReopenRoundHandler(roundRepository = get()) }
    single { FindBalanceHandler(walletPort = get()) }
    single { FindCasinoRoundQueryHandler() }
    single { FindAllCasinoRoundQueryHandler() }

    // CasinoGame
    single { PlayCasinoGameCommandHandler(gameRepository = get(), playerLimitPort = get(), openSessionUsecase = get()) }
    single { SaveCasinoGameCommandHandler(gameRepository = get()) }
    single { FindCasinoGameQueryHandler() }
    single { FindAllCasinoGameQueryHandler() }
    single { FindAllCasinoGameTagQueryHandler() }
    single { FindAllActiveRtpCasinoGameQueryHandler() }
    single { BatchCasinoGameQueryHandler() }
    single { GetCasinoGameDemoUrlQueryHandler(gameRepository = get(), gameHubClient = get()) }
    single { FindAllCasinoGamePlayerFavoriteQueryHandler() }
    single { FindAllCasinoGamePlayerLastQueryHandler() }
    single { FindAllCasinoGameCollectionQueryHandler() }
    single { AddCasinoGameFavouriteCommandHandler() }
    single { RemoveCasinoGameFavouriteCommandHandler() }

    // Freespin — thin proxies onto GameHubClient, no local state
    single { GetFreespinPresetsQueryHandler(gameRepository = get(), gameHubClient = get()) }
    single { CreateFreespinCommandHandler(gameHubClient = get()) }
    single { CancelFreespinCommandHandler(gameHubClient = get()) }

    // CasinoProvider
    single { SaveCasinoProviderCommandHandler(providerRepository = get()) }
    single { FindCasinoProviderQueryHandler() }
    single { FindAllCasinoProviderQueryHandler() }
    single { FindAllCasinoProviderTagQueryHandler() }
    single { BatchCasinoProviderQueryHandler() }

    // Collection
    single { SaveCollectionCommandHandler(collectionRepository = get()) }
    single { FindCollectionQueryHandler() }
    single { FindAllCollectionQueryHandler() }
    single { BatchCollectionQueryHandler() }
    single { AddCollectionCasinoGameCommandHandler(collectionRepository = get()) }
    single { RemoveCollectionCasinoGameCommandHandler(collectionRepository = get()) }
    single { UpdateCollectionCasinoGameOrderCommandHandler(collectionRepository = get()) }
    single { DeleteCollectionCommandHandler(collectionRepository = get()) }

    // Common (polymorphic — serves SetCasinoGameImageCommand / SetCasinoProviderImageCommand / SetCollectionImageCommand)
    single {
        SetImageCommandHandler(
            gameRepository = get(),
            providerRepository = get(),
            collectionRepository = get(),
        )
    }

    // Winner
    single { LastWinnerQueryHandler() }

    // Sportbook — thin proxy onto GameHubClient
    single { OpenSportbookHandler(gameHubClient = get()) }
}
