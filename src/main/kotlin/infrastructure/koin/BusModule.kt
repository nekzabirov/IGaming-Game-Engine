package infrastructure.koin

import application.Bus
import application.command.aggregator.DeleteAggregatorCommand
import application.command.aggregator.SaveAggregatorCommand
import application.command.aggregator.SyncAllActiveAggregatorCommand
import application.command.collection.AddCollectionCasinoGameCommand
import application.command.collection.DeleteCollectionCommand
import application.command.collection.RemoveCollectionCasinoGameCommand
import application.command.collection.SaveCollectionCommand
import application.command.collection.SetCollectionImageCommand
import application.command.collection.UpdateCollectionCasinoGameOrderCommand
import application.command.freespin.CancelFreespinCommand
import application.command.freespin.ChargeFreespinCommand
import application.command.freespin.CreateFreespinCommand
import application.command.game.AddCasinoGameFavouriteCommand
import application.command.game.PlayCasinoGameCommand
import application.command.game.RecalculateCasinoGameRtpCommand
import application.command.game.RemoveCasinoGameFavouriteCommand
import application.command.game.SaveCasinoGameCommand
import application.command.game.SetCasinoGameImageCommand
import application.command.provider.SaveCasinoProviderCommand
import application.command.provider.SetCasinoProviderImageCommand
import application.command.bet.ConfirmBetCommand
import application.command.bet.PlaceBetCommand
import application.command.bet.RollbackBetCommand
import application.command.bet.SettleBetCommand
import application.command.session.EndCasinoRoundSessionCommand
import application.command.session.PlaceSpinCasinoSessionCommand
import application.command.session.ReopenCasinoRoundSessionCommand
import application.command.session.RollbackSpinCasinoSessionCommand
import application.command.session.SettleSpinCasinoSessionCommand
import application.command.sportbook.ExchangeSportbookTokenCommand
import application.command.sportbook.OpenSportbookCommand
import application.command.wheel.CreditWheelCommand
import application.command.wheel.PayoutWheelCommand
import application.command.wheel.RollbackWheelCommand
import application.query.aggregator.BatchAggregatorQuery
import application.query.aggregator.FindAggregatorQuery
import application.query.aggregator.FindAllAggregatorQuery
import application.query.collection.BatchCollectionQuery
import application.query.collection.FindAllCollectionQuery
import application.query.collection.FindCollectionQuery
import application.query.freespin.FindRedeemableFreespinQuery
import application.query.freespin.GetFreespinPresetsQuery
import application.query.game.BatchCasinoGameQuery
import application.query.game.FindAllActiveRtpCasinoGameQuery
import application.query.game.FindAllCasinoGameCollectionQuery
import application.query.game.FindAllCasinoGamePlayerFavoriteQuery
import application.query.game.FindAllCasinoGamePlayerLastQuery
import application.query.game.FindAllCasinoGameQuery
import application.query.game.FindAllCasinoGameTagQuery
import application.query.game.FindCasinoGameQuery
import application.query.game.GetCasinoGameDemoUrlQuery
import application.query.provider.BatchCasinoProviderQuery
import application.query.provider.FindAllCasinoProviderQuery
import application.query.provider.FindAllCasinoProviderTagQuery
import application.query.provider.FindCasinoProviderQuery
import application.query.round.FindAllCasinoRoundQuery
import application.query.round.FindCasinoRoundQuery
import application.query.session.FindCasinoSessionBalanceQuery
import application.query.session.FindCasinoSessionByExternalTokenQuery
import application.query.session.FindCasinoSessionQuery
import application.query.sportbook.FindActiveSportbookAggregatorQuery
import application.query.sportbook.FindLastSportbookSessionByPlayerQuery
import application.query.sportbook.FindSportbookSessionByPrivateTokenQuery
import application.query.sportbook.FindSportbookSessionQuery
import application.query.sportbook.InitSportbookQuery
import application.query.winner.LastWinnerQuery
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
import infrastructure.handler.session.FindCasinoSessionBalanceHandler
import infrastructure.handler.session.FindCasinoSessionByExternalTokenHandler
import infrastructure.handler.session.FindCasinoSessionHandler
import infrastructure.handler.session.PlaceSpinCasinoSessionHandler
import infrastructure.handler.session.ReopenCasinoRoundSessionHandler
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
 * Explicit class-to-handler routing for the [Bus]. Add a new command/query here
 * and in [handlerModule] — these are the only two files that change when you
 * add a new CQRS handler. The polymorphic [SetImageCommandHandler] is wired under
 * three concrete sealed subtypes (game/provider/collection image) — same instance,
 * three map entries.
 */
val busModule = module {
    single<Bus> {
        val setImageHandler = get<SetImageCommandHandler>()

        BusImpl(
            commandHandlers = mapOf(
                // CasinoSession
                PlaceSpinCasinoSessionCommand::class.java to get<PlaceSpinCasinoSessionHandler>(),
                SettleSpinCasinoSessionCommand::class.java to get<SettleSpinCasinoSessionHandler>(),
                RollbackSpinCasinoSessionCommand::class.java to get<RollbackSpinCasinoSessionHandler>(),
                EndCasinoRoundSessionCommand::class.java to get<EndCasinoRoundSessionHandler>(),
                ReopenCasinoRoundSessionCommand::class.java to get<ReopenCasinoRoundSessionHandler>(),
                // CasinoGame
                PlayCasinoGameCommand::class.java to get<PlayCasinoGameCommandHandler>(),
                SaveCasinoGameCommand::class.java to get<SaveCasinoGameCommandHandler>(),
                RecalculateCasinoGameRtpCommand::class.java to get<RecalculateCasinoGameRtpCommandHandler>(),
                SetCasinoGameImageCommand::class.java to setImageHandler,
                AddCasinoGameFavouriteCommand::class.java to get<AddCasinoGameFavouriteCommandHandler>(),
                RemoveCasinoGameFavouriteCommand::class.java to get<RemoveCasinoGameFavouriteCommandHandler>(),
                // Freespin
                CreateFreespinCommand::class.java to get<CreateFreespinCommandHandler>(),
                CancelFreespinCommand::class.java to get<CancelFreespinCommandHandler>(),
                ChargeFreespinCommand::class.java to get<ChargeFreespinCommandHandler>(),
                // CasinoProvider
                SaveCasinoProviderCommand::class.java to get<SaveCasinoProviderCommandHandler>(),
                SetCasinoProviderImageCommand::class.java to setImageHandler,
                // Collection
                SaveCollectionCommand::class.java to get<SaveCollectionCommandHandler>(),
                SetCollectionImageCommand::class.java to setImageHandler,
                AddCollectionCasinoGameCommand::class.java to get<AddCollectionCasinoGameCommandHandler>(),
                RemoveCollectionCasinoGameCommand::class.java to get<RemoveCollectionCasinoGameCommandHandler>(),
                UpdateCollectionCasinoGameOrderCommand::class.java to get<UpdateCollectionCasinoGameOrderCommandHandler>(),
                DeleteCollectionCommand::class.java to get<DeleteCollectionCommandHandler>(),
                // Aggregator
                SaveAggregatorCommand::class.java to get<SaveAggregatorCommandHandler>(),
                DeleteAggregatorCommand::class.java to get<DeleteAggregatorCommandHandler>(),
                SyncAllActiveAggregatorCommand::class.java to get<SyncAllActiveAggregatorCommandHandler>(),
                // Sportbook
                OpenSportbookCommand::class.java to get<OpenSportbookHandler>(),
                ExchangeSportbookTokenCommand::class.java to get<ExchangeSportbookTokenHandler>(),
                PlaceBetCommand::class.java to get<PlaceBetHandler>(),
                ConfirmBetCommand::class.java to get<ConfirmBetHandler>(),
                SettleBetCommand::class.java to get<SettleBetHandler>(),
                RollbackBetCommand::class.java to get<RollbackBetHandler>(),
                CreditWheelCommand::class.java to get<CreditWheelHandler>(),
                PayoutWheelCommand::class.java to get<PayoutWheelHandler>(),
                RollbackWheelCommand::class.java to get<RollbackWheelHandler>(),
            ),
            queryHandlers = mapOf(
                FindCasinoSessionQuery::class.java to get<FindCasinoSessionHandler>(),
                FindCasinoSessionByExternalTokenQuery::class.java to get<FindCasinoSessionByExternalTokenHandler>(),
                FindCasinoSessionBalanceQuery::class.java to get<FindCasinoSessionBalanceHandler>(),
                FindCasinoGameQuery::class.java to get<FindCasinoGameQueryHandler>(),
                FindAllCasinoGameQuery::class.java to get<FindAllCasinoGameQueryHandler>(),
                FindAllCasinoGameTagQuery::class.java to get<FindAllCasinoGameTagQueryHandler>(),
                FindAllActiveRtpCasinoGameQuery::class.java to get<FindAllActiveRtpCasinoGameQueryHandler>(),
                BatchCasinoGameQuery::class.java to get<BatchCasinoGameQueryHandler>(),
                FindAllCasinoGamePlayerFavoriteQuery::class.java to get<FindAllCasinoGamePlayerFavoriteQueryHandler>(),
                FindAllCasinoGamePlayerLastQuery::class.java to get<FindAllCasinoGamePlayerLastQueryHandler>(),
                FindAllCasinoGameCollectionQuery::class.java to get<FindAllCasinoGameCollectionQueryHandler>(),
                GetCasinoGameDemoUrlQuery::class.java to get<GetCasinoGameDemoUrlQueryHandler>(),
                GetFreespinPresetsQuery::class.java to get<GetFreespinPresetsQueryHandler>(),
                FindRedeemableFreespinQuery::class.java to get<FindRedeemableFreespinQueryHandler>(),
                FindCasinoProviderQuery::class.java to get<FindCasinoProviderQueryHandler>(),
                FindAllCasinoProviderQuery::class.java to get<FindAllCasinoProviderQueryHandler>(),
                FindAllCasinoProviderTagQuery::class.java to get<FindAllCasinoProviderTagQueryHandler>(),
                BatchCasinoProviderQuery::class.java to get<BatchCasinoProviderQueryHandler>(),
                FindCollectionQuery::class.java to get<FindCollectionQueryHandler>(),
                FindAllCollectionQuery::class.java to get<FindAllCollectionQueryHandler>(),
                BatchCollectionQuery::class.java to get<BatchCollectionQueryHandler>(),
                FindAggregatorQuery::class.java to get<FindAggregatorQueryHandler>(),
                FindAllAggregatorQuery::class.java to get<FindAllAggregatorQueryHandler>(),
                BatchAggregatorQuery::class.java to get<BatchAggregatorQueryHandler>(),
                FindCasinoRoundQuery::class.java to get<FindCasinoRoundQueryHandler>(),
                FindAllCasinoRoundQuery::class.java to get<FindAllCasinoRoundQueryHandler>(),
                LastWinnerQuery::class.java to get<LastWinnerQueryHandler>(),
                FindSportbookSessionQuery::class.java to get<FindSportbookSessionHandler>(),
                FindSportbookSessionByPrivateTokenQuery::class.java to get<FindSportbookSessionByPrivateTokenHandler>(),
                FindLastSportbookSessionByPlayerQuery::class.java to get<FindLastSportbookSessionByPlayerHandler>(),
                FindActiveSportbookAggregatorQuery::class.java to get<FindActiveSportbookAggregatorHandler>(),
                InitSportbookQuery::class.java to get<InitSportbookHandler>(),
            ),
        )
    }
}
