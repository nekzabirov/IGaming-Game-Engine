package infrastructure.koin

import application.Bus
import application.command.aggregator.DeleteAggregatorCommand
import application.command.aggregator.SaveAggregatorCommand
import application.command.aggregator.SyncAllActiveAggregatorCommand
import application.command.collection.SaveCollectionCommand
import application.command.collection.SetCollectionImageCommand
import application.command.collection.UpdateCollectionGameCommand
import application.command.freespin.CancelFreespinCommand
import application.command.freespin.CreateFreespinCommand
import application.command.game.AddGameFavouriteCommand
import application.command.game.PlayGameCommand
import application.command.game.RemoveGameFavouriteCommand
import application.command.game.SaveGameCommand
import application.command.game.SetGameImageCommand
import application.command.provider.SaveProviderCommand
import application.command.provider.SetProviderImageCommand
import application.command.session.EndRoundSessionCommand
import application.command.session.PlaceSpinSessionCommand
import application.command.session.SettleSpinSessionCommand
import application.query.aggregator.BatchAggregatorQuery
import application.query.aggregator.FindAggregatorQuery
import application.query.aggregator.FindAllAggregatorQuery
import application.query.collection.BatchCollectionQuery
import application.query.collection.FindAllCollectionQuery
import application.query.collection.FindCollectionQuery
import application.query.freespin.GetFreespinPresetsQuery
import application.query.game.BatchGameQuery
import application.query.game.FindAllGameCollectionQuery
import application.query.game.FindAllGamePlayerFavoriteQuery
import application.query.game.FindAllGameQuery
import application.query.game.FindGameQuery
import application.query.game.GetGameDemoUrlQuery
import application.query.provider.BatchProviderQuery
import application.query.provider.FindAllProviderQuery
import application.query.provider.FindProviderQuery
import application.query.round.FindAllRoundQuery
import application.query.round.FindRoundQuery
import application.query.session.FindSessionBalanceQuery
import application.query.winner.LastWinnerQuery
import infrastructure.handler.aggregator.BatchAggregatorQueryHandler
import infrastructure.handler.aggregator.DeleteAggregatorCommandHandler
import infrastructure.handler.aggregator.FindAggregatorQueryHandler
import infrastructure.handler.aggregator.FindAllAggregatorQueryHandler
import infrastructure.handler.aggregator.SaveAggregatorCommandHandler
import infrastructure.handler.aggregator.SyncAllActiveAggregatorCommandHandler
import infrastructure.handler.collection.BatchCollectionQueryHandler
import infrastructure.handler.collection.FindAllCollectionQueryHandler
import infrastructure.handler.collection.FindCollectionQueryHandler
import infrastructure.handler.collection.SaveCollectionCommandHandler
import infrastructure.handler.collection.UpdateCollectionGameCommandHandler
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
                // Session
                PlaceSpinSessionCommand::class.java to get<PlaceSpinSessionHandler>(),
                SettleSpinSessionCommand::class.java to get<SettleSpinSessionHandler>(),
                EndRoundSessionCommand::class.java to get<EndRoundSessionHandler>(),
                // Game
                PlayGameCommand::class.java to get<PlayGameCommandHandler>(),
                SaveGameCommand::class.java to get<SaveGameCommandHandler>(),
                SetGameImageCommand::class.java to setImageHandler,
                AddGameFavouriteCommand::class.java to get<AddGameFavouriteCommandHandler>(),
                RemoveGameFavouriteCommand::class.java to get<RemoveGameFavouriteCommandHandler>(),
                // Freespin
                CreateFreespinCommand::class.java to get<CreateFreespinCommandHandler>(),
                CancelFreespinCommand::class.java to get<CancelFreespinCommandHandler>(),
                // Provider
                SaveProviderCommand::class.java to get<SaveProviderCommandHandler>(),
                SetProviderImageCommand::class.java to setImageHandler,
                // Collection
                SaveCollectionCommand::class.java to get<SaveCollectionCommandHandler>(),
                SetCollectionImageCommand::class.java to setImageHandler,
                UpdateCollectionGameCommand::class.java to get<UpdateCollectionGameCommandHandler>(),
                // Aggregator
                SaveAggregatorCommand::class.java to get<SaveAggregatorCommandHandler>(),
                DeleteAggregatorCommand::class.java to get<DeleteAggregatorCommandHandler>(),
                SyncAllActiveAggregatorCommand::class.java to get<SyncAllActiveAggregatorCommandHandler>(),
            ),
            queryHandlers = mapOf(
                FindSessionBalanceQuery::class.java to get<FindSessionBalanceHandler>(),
                FindGameQuery::class.java to get<FindGameQueryHandler>(),
                FindAllGameQuery::class.java to get<FindAllGameQueryHandler>(),
                BatchGameQuery::class.java to get<BatchGameQueryHandler>(),
                FindAllGamePlayerFavoriteQuery::class.java to get<FindAllGamePlayerFavoriteQueryHandler>(),
                FindAllGameCollectionQuery::class.java to get<FindAllGameCollectionQueryHandler>(),
                GetGameDemoUrlQuery::class.java to get<GetGameDemoUrlQueryHandler>(),
                GetFreespinPresetsQuery::class.java to get<GetFreespinPresetsQueryHandler>(),
                FindProviderQuery::class.java to get<FindProviderQueryHandler>(),
                FindAllProviderQuery::class.java to get<FindAllProviderQueryHandler>(),
                BatchProviderQuery::class.java to get<BatchProviderQueryHandler>(),
                FindCollectionQuery::class.java to get<FindCollectionQueryHandler>(),
                FindAllCollectionQuery::class.java to get<FindAllCollectionQueryHandler>(),
                BatchCollectionQuery::class.java to get<BatchCollectionQueryHandler>(),
                FindAggregatorQuery::class.java to get<FindAggregatorQueryHandler>(),
                FindAllAggregatorQuery::class.java to get<FindAllAggregatorQueryHandler>(),
                BatchAggregatorQuery::class.java to get<BatchAggregatorQueryHandler>(),
                FindRoundQuery::class.java to get<FindRoundQueryHandler>(),
                FindAllRoundQuery::class.java to get<FindAllRoundQueryHandler>(),
                LastWinnerQuery::class.java to get<LastWinnerQueryHandler>(),
            ),
        )
    }
}
