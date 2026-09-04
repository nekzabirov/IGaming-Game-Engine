package infrastructure.koin

import infrastructure.handler.aggregator.BatchAggregatorQueryHandler
import infrastructure.handler.aggregator.DeleteAggregatorCommandHandler
import infrastructure.handler.aggregator.FindAggregatorQueryHandler
import infrastructure.handler.aggregator.FindAllAggregatorQueryHandler
import infrastructure.handler.aggregator.SaveAggregatorCommandHandler
import infrastructure.handler.aggregator.SyncAllActiveAggregatorCommandHandler
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
import infrastructure.handler.freespin.ChargeFreespinCommandHandler
import infrastructure.handler.freespin.CreateFreespinCommandHandler
import infrastructure.handler.freespin.FindRedeemableFreespinQueryHandler
import infrastructure.handler.game.AddCasinoGameFavouriteCommandHandler
import infrastructure.handler.game.BatchCasinoGameQueryHandler
import infrastructure.handler.game.FindAllActiveRtpCasinoGameQueryHandler
import infrastructure.handler.game.FindAllCasinoGameCollectionQueryHandler
import infrastructure.handler.game.FindAllCasinoGamePlayerFavoriteQueryHandler
import infrastructure.handler.game.FindAllCasinoGamePlayerLastQueryHandler
import infrastructure.handler.game.FindAllCasinoGameQueryHandler
import infrastructure.handler.game.FindAllCasinoGameTagQueryHandler
import infrastructure.handler.game.FindCasinoGameQueryHandler
import infrastructure.handler.game.GetFreespinPresetsQueryHandler
import infrastructure.handler.game.GetCasinoGameDemoUrlQueryHandler
import infrastructure.handler.game.PlayCasinoGameCommandHandler
import infrastructure.handler.game.RecalculateCasinoGameRtpCommandHandler
import infrastructure.handler.game.RemoveCasinoGameFavouriteCommandHandler
import infrastructure.handler.game.SaveCasinoGameCommandHandler
import infrastructure.handler.provider.BatchCasinoProviderQueryHandler
import infrastructure.handler.provider.FindAllCasinoProviderQueryHandler
import infrastructure.handler.provider.FindAllCasinoProviderTagQueryHandler
import infrastructure.handler.provider.FindCasinoProviderQueryHandler
import infrastructure.handler.provider.SaveCasinoProviderCommandHandler
import infrastructure.handler.round.FindAllCasinoRoundQueryHandler
import infrastructure.handler.round.FindCasinoRoundQueryHandler
import infrastructure.handler.session.EndCasinoRoundSessionHandler
import infrastructure.handler.session.ReopenCasinoRoundSessionHandler
import infrastructure.handler.session.FindCasinoSessionBalanceHandler
import infrastructure.handler.session.FindCasinoSessionByExternalTokenHandler
import infrastructure.handler.session.FindCasinoSessionHandler
import infrastructure.handler.session.PlaceSpinCasinoSessionHandler
import infrastructure.handler.session.RollbackSpinCasinoSessionHandler
import infrastructure.handler.session.SettleSpinCasinoSessionHandler
import infrastructure.handler.bet.ConfirmBetHandler
import infrastructure.handler.bet.PlaceBetHandler
import infrastructure.handler.bet.RollbackBetHandler
import infrastructure.handler.bet.SettleBetHandler
import infrastructure.handler.sportbook.ExchangeSportbookTokenHandler
import infrastructure.handler.sportbook.FindActiveSportbookAggregatorHandler
import infrastructure.handler.sportbook.FindLastSportbookSessionByPlayerHandler
import infrastructure.handler.sportbook.FindSportbookSessionByPrivateTokenHandler
import infrastructure.handler.sportbook.FindSportbookSessionHandler
import infrastructure.handler.sportbook.InitSportbookHandler
import infrastructure.handler.sportbook.OpenSportbookHandler
import infrastructure.handler.wheel.CreditWheelHandler
import infrastructure.handler.wheel.PayoutWheelHandler
import infrastructure.handler.wheel.RollbackWheelHandler
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
    // CasinoSession
    single { FindCasinoSessionHandler(sessionRepository = get()) }
    single { FindCasinoSessionByExternalTokenHandler(sessionRepository = get()) }
    single { PlaceSpinCasinoSessionHandler(roundRepository = get(), spinRepository = get(), processSpinUsecase = get(), walletPort = get()) }
    single { SettleSpinCasinoSessionHandler(roundRepository = get(), spinRepository = get(), processSpinUsecase = get(), walletPort = get()) }
    single { RollbackSpinCasinoSessionHandler(spinRepository = get(), processSpinUsecase = get(), walletPort = get()) }
    single { EndCasinoRoundSessionHandler(roundRepository = get(), finishRoundUsecase = get()) }
    single { ReopenCasinoRoundSessionHandler(roundRepository = get()) }
    single { FindCasinoSessionBalanceHandler(walletAdapter = get()) }

    // CasinoGame
    single {
        PlayCasinoGameCommandHandler(
            gameVariantRepository = get(),
            aggregatorRepository = get(),
            playerLimitPort = get(),
            openSessionUsecase = get(),
        )
    }
    single { SaveCasinoGameCommandHandler(gameRepository = get(), providerRepository = get()) }
    single { RecalculateCasinoGameRtpCommandHandler(recalculateCasinoGameRtpUsecase = get()) }
    single { FindCasinoGameQueryHandler() }
    single { FindAllCasinoGameQueryHandler() }
    single { FindAllCasinoGameTagQueryHandler() }
    single { FindAllActiveRtpCasinoGameQueryHandler() }
    single { BatchCasinoGameQueryHandler() }
    single { GetCasinoGameDemoUrlQueryHandler(gameVariantRepository = get(), aggregatorRepository = get(), aggregatorFactory = get()) }
    single { FindAllCasinoGamePlayerFavoriteQueryHandler() }
    single { FindAllCasinoGamePlayerLastQueryHandler() }
    single { FindAllCasinoGameCollectionQueryHandler() }
    single { AddCasinoGameFavouriteCommandHandler() }
    single { RemoveCasinoGameFavouriteCommandHandler() }

    // Freespin
    single { GetFreespinPresetsQueryHandler(aggregatorFactory = get()) }
    single {
        CreateFreespinCommandHandler(
            gameVariantRepository = get(),
            freespinRepository = get(),
            aggregatorRepository = get(),
            aggregatorFactory = get(),
        )
    }
    single {
        CancelFreespinCommandHandler(
            gameVariantRepository = get(),
            freespinRepository = get(),
            aggregatorRepository = get(),
            aggregatorFactory = get(),
        )
    }
    single { ChargeFreespinCommandHandler(freespinRepository = get()) }
    single { FindRedeemableFreespinQueryHandler(freespinRepository = get()) }

    // CasinoProvider
    single { SaveCasinoProviderCommandHandler(providerRepository = get(), aggregatorRepository = get()) }
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

    // Aggregator
    single { SaveAggregatorCommandHandler(aggregatorRepository = get()) }
    single { DeleteAggregatorCommandHandler(aggregatorRepository = get()) }
    single { FindAggregatorQueryHandler() }
    single { FindAllAggregatorQueryHandler() }
    single { BatchAggregatorQueryHandler(aggregatorRepository = get()) }
    single { SyncAllActiveAggregatorCommandHandler(get(), get()) }

    // CasinoRound
    single { FindCasinoRoundQueryHandler() }
    single { FindAllCasinoRoundQueryHandler() }

    // Winner
    single { LastWinnerQueryHandler() }

    // Sportbook
    single { FindSportbookSessionHandler(sessionRepository = get()) }
    single { FindSportbookSessionByPrivateTokenHandler(sessionRepository = get()) }
    single { FindLastSportbookSessionByPlayerHandler(sessionRepository = get()) }
    single { FindActiveSportbookAggregatorHandler(aggregatorRepository = get()) }
    single { InitSportbookHandler(aggregatorRepository = get(), aggregatorFactory = get()) }
    single { ExchangeSportbookTokenHandler(sessionRepository = get()) }
    single { OpenSportbookHandler(openSportbookUsecase = get()) }

    // Bet
    single { PlaceBetHandler(processBetUsecase = get()) }
    single { ConfirmBetHandler(processBetUsecase = get()) }
    single { SettleBetHandler(processBetUsecase = get()) }
    single { RollbackBetHandler(processBetUsecase = get()) }

    // Wheel
    single { CreditWheelHandler(processWheelUsecase = get()) }
    single { PayoutWheelHandler(processWheelUsecase = get()) }
    single { RollbackWheelHandler(processWheelUsecase = get()) }
}
