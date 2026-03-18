package infrastructure.koin

import application.cqrs.Bus
import application.cqrs.ICommandHandler
import application.cqrs.IQueryHandler
import application.cqrs.aggregator.FindAggregatorQuery
import application.cqrs.aggregator.FindAllAggregatorQuery
import application.cqrs.aggregator.SaveAggregatorCommand
import application.cqrs.aggregator.SyncAllActiveAggregatorCommand
import application.cqrs.collection.FindAllCollectionQuery
import application.cqrs.collection.FindCollectionQuery
import application.cqrs.collection.SaveCollectionCommand
import application.cqrs.collection.SetCollectionImageCommand
import application.cqrs.collection.UpdateCollectionGameCommand
import application.cqrs.freespin.CancelFreespinCommand
import application.cqrs.freespin.CreateFreespinCommand
import application.cqrs.freespin.GetFreespinPresetsQuery
import application.cqrs.game.AddGameFavouriteCommand
import application.cqrs.game.FindAllGamePlayerFavoriteQuery
import application.cqrs.game.BatchGameQuery
import application.cqrs.game.FindAllGameQuery
import application.cqrs.game.FindGameQuery
import application.cqrs.game.GetGameDemoUrlQuery
import application.cqrs.game.PlayGameCommand
import application.cqrs.game.RemoveGameFavouriteCommand
import application.cqrs.game.SaveGameCommand
import application.cqrs.game.SetGameImageCommand
import application.cqrs.provider.FindAllProviderQuery
import application.cqrs.provider.FindProviderQuery
import application.cqrs.provider.SaveProviderCommand
import application.cqrs.provider.SetProviderImageCommand
import application.cqrs.round.FindAllRoundQuery
import application.cqrs.round.FindRoundQuery
import application.cqrs.session.EndRoundSessionCommand
import application.cqrs.session.FindSessionBalanceQuery
import application.cqrs.session.PlaceSpinSessionCommand
import application.cqrs.session.SettleSpinSessionCommand
import application.cqrs.winner.LastWinnerQuery
import infrastructure.handler.aggregator.FindAggregatorQueryHandler
import infrastructure.handler.aggregator.FindAllAggregatorQueryHandler
import infrastructure.handler.aggregator.SaveAggregatorCommandHandler
import infrastructure.handler.aggregator.SyncAllActiveAggregatorCommandHandler
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

val busModule = module {
    single<Bus> {
        BusImpl(
            commandHandlers = mapOf(
                PlaceSpinSessionCommand::class.java to get<PlaceSpinSessionHandler>(),
                SettleSpinSessionCommand::class.java to get<SettleSpinSessionHandler>(),
                EndRoundSessionCommand::class.java to get<EndRoundSessionHandler>(),
                PlayGameCommand::class.java to get<PlayGameCommandHandler>(),
                SaveGameCommand::class.java to get<SaveGameCommandHandler>(),
                SetGameImageCommand::class.java to get<SetGameImageCommandHandler>(),
                AddGameFavouriteCommand::class.java to get<AddGameFavouriteCommandHandler>(),
                RemoveGameFavouriteCommand::class.java to get<RemoveGameFavouriteCommandHandler>(),
                CreateFreespinCommand::class.java to get<CreateFreespinCommandHandler>(),
                CancelFreespinCommand::class.java to get<CancelFreespinCommandHandler>(),
                SaveProviderCommand::class.java to get<SaveProviderCommandHandler>(),
                SetProviderImageCommand::class.java to get<SetProviderImageCommandHandler>(),
                SaveCollectionCommand::class.java to get<SaveCollectionCommandHandler>(),
                SetCollectionImageCommand::class.java to get<SetCollectionImageCommandHandler>(),
                UpdateCollectionGameCommand::class.java to get<UpdateCollectionGameCommandHandler>(),
                SaveAggregatorCommand::class.java to get<SaveAggregatorCommandHandler>(),
                SyncAllActiveAggregatorCommand::class.java to get<SyncAllActiveAggregatorCommandHandler>()
            ),
            queryHandlers = mapOf(
                FindSessionBalanceQuery::class.java to get<FindSessionBalanceHandler>(),
                FindGameQuery::class.java to get<FindGameQueryHandler>(),
                FindAllGameQuery::class.java to get<FindAllGameQueryHandler>(),
                BatchGameQuery::class.java to get<BatchGameQueryHandler>(),
                FindAllGamePlayerFavoriteQuery::class.java to get<FindAllGamePlayerFavoriteQueryHandler>(),
                GetGameDemoUrlQuery::class.java to get<GetGameDemoUrlQueryHandler>(),
                GetFreespinPresetsQuery::class.java to get<GetFreespinPresetsQueryHandler>(),
                FindProviderQuery::class.java to get<FindProviderQueryHandler>(),
                FindAllProviderQuery::class.java to get<FindAllProviderQueryHandler>(),
                FindCollectionQuery::class.java to get<FindCollectionQueryHandler>(),
                FindAllCollectionQuery::class.java to get<FindAllCollectionQueryHandler>(),
                FindAggregatorQuery::class.java to get<FindAggregatorQueryHandler>(),
                FindAllAggregatorQuery::class.java to get<FindAllAggregatorQueryHandler>(),
                FindRoundQuery::class.java to get<FindRoundQueryHandler>(),
                FindAllRoundQuery::class.java to get<FindAllRoundQueryHandler>(),
                LastWinnerQuery::class.java to get<LastWinnerQueryHandler>(),
            )
        )
    }
}
