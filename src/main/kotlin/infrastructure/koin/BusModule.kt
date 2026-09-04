package infrastructure.koin

import application.Bus
import application.command.collection.AddCollectionCasinoGameCommand
import application.command.collection.DeleteCollectionCommand
import application.command.collection.RemoveCollectionCasinoGameCommand
import application.command.collection.SaveCollectionCommand
import application.command.collection.SetCollectionImageCommand
import application.command.collection.UpdateCollectionCasinoGameOrderCommand
import application.command.freespin.CancelFreespinCommand
import application.command.freespin.CreateFreespinCommand
import application.command.game.AddCasinoGameFavouriteCommand
import application.command.game.PlayCasinoGameCommand
import application.command.game.RemoveCasinoGameFavouriteCommand
import application.command.game.SaveCasinoGameCommand
import application.command.game.SetCasinoGameImageCommand
import application.command.provider.SaveCasinoProviderCommand
import application.command.provider.SetCasinoProviderImageCommand
import application.command.round.CloseRoundCommand
import application.command.round.PlaceSpinCommand
import application.command.round.ReopenRoundCommand
import application.command.round.RollbackSpinCommand
import application.command.round.SettleSpinCommand
import application.command.sportbook.OpenSportbookCommand
import application.query.collection.BatchCollectionQuery
import application.query.collection.FindAllCollectionQuery
import application.query.collection.FindCollectionQuery
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
import application.query.round.FindBalanceQuery
import application.query.round.FindCasinoRoundQuery
import application.query.winner.LastWinnerQuery
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
                // CasinoRound / wallet
                PlaceSpinCommand::class.java to get<PlaceSpinHandler>(),
                SettleSpinCommand::class.java to get<SettleSpinHandler>(),
                RollbackSpinCommand::class.java to get<RollbackSpinHandler>(),
                CloseRoundCommand::class.java to get<CloseRoundHandler>(),
                ReopenRoundCommand::class.java to get<ReopenRoundHandler>(),
                // CasinoGame
                PlayCasinoGameCommand::class.java to get<PlayCasinoGameCommandHandler>(),
                SaveCasinoGameCommand::class.java to get<SaveCasinoGameCommandHandler>(),
                SetCasinoGameImageCommand::class.java to setImageHandler,
                AddCasinoGameFavouriteCommand::class.java to get<AddCasinoGameFavouriteCommandHandler>(),
                RemoveCasinoGameFavouriteCommand::class.java to get<RemoveCasinoGameFavouriteCommandHandler>(),
                // Freespin
                CreateFreespinCommand::class.java to get<CreateFreespinCommandHandler>(),
                CancelFreespinCommand::class.java to get<CancelFreespinCommandHandler>(),
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
                // Sportbook
                OpenSportbookCommand::class.java to get<OpenSportbookHandler>(),
            ),
            queryHandlers = mapOf(
                FindBalanceQuery::class.java to get<FindBalanceHandler>(),
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
                FindCasinoProviderQuery::class.java to get<FindCasinoProviderQueryHandler>(),
                FindAllCasinoProviderQuery::class.java to get<FindAllCasinoProviderQueryHandler>(),
                FindAllCasinoProviderTagQuery::class.java to get<FindAllCasinoProviderTagQueryHandler>(),
                BatchCasinoProviderQuery::class.java to get<BatchCasinoProviderQueryHandler>(),
                FindCollectionQuery::class.java to get<FindCollectionQueryHandler>(),
                FindAllCollectionQuery::class.java to get<FindAllCollectionQueryHandler>(),
                BatchCollectionQuery::class.java to get<BatchCollectionQueryHandler>(),
                FindCasinoRoundQuery::class.java to get<FindCasinoRoundQueryHandler>(),
                FindAllCasinoRoundQuery::class.java to get<FindAllCasinoRoundQueryHandler>(),
                LastWinnerQuery::class.java to get<LastWinnerQueryHandler>(),
            ),
        )
    }
}
