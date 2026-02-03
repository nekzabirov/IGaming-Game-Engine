package com.nekgamebling.infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import application.port.inbound.command.AddGameTagCommand
import application.port.inbound.command.RemoveGameTagCommand
import application.port.inbound.command.UpdateGameCommand
import application.port.inbound.command.UpdateGameImageCommand
import com.nekgamebling.application.port.inbound.game.query.FindAllGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindAllGameResponse
import com.nekgamebling.application.port.inbound.game.query.FindGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindGameResponse
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlQuery
import com.nekgamebling.application.port.inbound.game.query.GameDemoUrlResponse
import com.nekgamebling.application.port.inbound.aggregator.CreateAggregatorCommand
import com.nekgamebling.application.port.inbound.aggregator.CreateAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.FindAggregatorQuery
import com.nekgamebling.application.port.inbound.aggregator.FindAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.FindAllAggregatorQuery
import com.nekgamebling.application.port.inbound.aggregator.FindAllAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.SyncAllAggregatorCommand
import com.nekgamebling.application.port.inbound.aggregator.SyncAllAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.UpdateAggregatorCommand
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersQuery
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersResponse
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderQuery
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderResponse
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderCommand
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderImageCommand
import com.nekgamebling.application.port.inbound.collection.command.CreateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.CreateCollectionResponse
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionGamesCommand
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsQuery
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsResponse
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionQuery
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionResponse
import com.nekgamebling.infrastructure.handler.provider.command.UpdateProviderCommandHandler
import com.nekgamebling.infrastructure.handler.provider.command.UpdateProviderImageCommandHandler
import com.nekgamebling.infrastructure.handler.collection.command.CreateCollectionCommandHandler
import com.nekgamebling.infrastructure.handler.collection.command.UpdateCollectionCommandHandler
import com.nekgamebling.infrastructure.handler.collection.command.UpdateCollectionGamesCommandHandler
import com.nekgamebling.application.port.inbound.game.command.PlayGameCommand
import com.nekgamebling.application.port.inbound.game.command.PlayGameResponse
import com.nekgamebling.infrastructure.handler.game.command.PlayGameCommandHandler
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQueryResult
import com.nekgamebling.application.port.inbound.spin.FindRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindRoundQueryResult
import com.nekgamebling.infrastructure.handler.spin.query.FindAllRoundQueryHandler
import com.nekgamebling.infrastructure.handler.spin.query.FindRoundQueryHandler
import com.nekgamebling.infrastructure.handler.collection.query.FindAllCollectionsQueryHandler
import com.nekgamebling.infrastructure.handler.collection.query.FindCollectionQueryHandler
import com.nekgamebling.infrastructure.handler.game.query.FindAllGameQueryHandler
import com.nekgamebling.infrastructure.handler.aggregator.command.CreateAggregatorCommandHandler
import com.nekgamebling.infrastructure.handler.aggregator.command.SyncAggregatorCommandHandler
import com.nekgamebling.infrastructure.handler.aggregator.command.UpdateAggregatorCommandHandler
import com.nekgamebling.infrastructure.handler.aggregator.query.FindAggregatorQueryHandler
import com.nekgamebling.infrastructure.handler.aggregator.query.FindAllAggregatorQueryHandler
import com.nekgamebling.infrastructure.handler.provider.query.FindAllProvidersQueryHandler
import com.nekgamebling.infrastructure.handler.provider.query.FindaProviderQueryHandler
import com.nekgamebling.infrastructure.handler.game.query.FindGameQueryHandler
import infrastructure.handler.AddGameTagCommandHandler
import infrastructure.handler.GameDemoUrlQueryHandler
import infrastructure.handler.RemoveGameTagCommandHandler
import infrastructure.handler.UpdateGameCommandHandler
import infrastructure.handler.UpdateGameImageCommandHandler
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for query and command handlers.
 * Handlers provide direct database access for read/write operations.
 */
val handlerModule = module {
    // ==========================================
    // Query Handlers
    // ==========================================
    single<QueryHandler<FindGameQuery, FindGameResponse>>(named("findGame")) { FindGameQueryHandler() }
    single<QueryHandler<FindAllGameQuery, FindAllGameResponse>>(named("findAllGames")) { FindAllGameQueryHandler() }
    single<QueryHandler<GameDemoUrlQuery, GameDemoUrlResponse>>(named("gameDemoUrl")) { GameDemoUrlQueryHandler(get()) }
    single<QueryHandler<FindaProviderQuery, FindaProviderResponse>>(named("findProvider")) { FindaProviderQueryHandler() }
    single<QueryHandler<FindAllProvidersQuery, FindAllProvidersResponse>>(named("findAllProviders")) { FindAllProvidersQueryHandler() }
    single<QueryHandler<FindAggregatorQuery, FindAggregatorResponse>>(named("findAggregator")) { FindAggregatorQueryHandler() }
    single<QueryHandler<FindAllAggregatorQuery, FindAllAggregatorResponse>>(named("findAllAggregators")) { FindAllAggregatorQueryHandler() }
    single<QueryHandler<FindCollectionQuery, FindCollectionResponse>>(named("findCollection")) { FindCollectionQueryHandler() }
    single<QueryHandler<FindAllCollectionsQuery, FindAllCollectionsResponse>>(named("findAllCollections")) { FindAllCollectionsQueryHandler() }
    single<QueryHandler<FindRoundQuery, FindRoundQueryResult>>(named("findRound")) { FindRoundQueryHandler() }
    single<QueryHandler<FindAllRoundQuery, FindAllRoundQueryResult>>(named("findAllRounds")) { FindAllRoundQueryHandler() }

    // ==========================================
    // Command Handlers
    // ==========================================
    single<CommandHandler<CreateAggregatorCommand, CreateAggregatorResponse>>(named("createAggregator")) { CreateAggregatorCommandHandler() }
    single<CommandHandler<UpdateAggregatorCommand, Unit>>(named("updateAggregator")) { UpdateAggregatorCommandHandler() }
    single<CommandHandler<UpdateGameCommand, Unit>>(named("updateGame")) { UpdateGameCommandHandler() }
    single<CommandHandler<UpdateGameImageCommand, Unit>>(named("updateGameImage")) { UpdateGameImageCommandHandler(get()) }
    single<CommandHandler<AddGameTagCommand, Unit>>(named("addGameTag")) { AddGameTagCommandHandler() }
    single<CommandHandler<RemoveGameTagCommand, Unit>>(named("removeGameTag")) { RemoveGameTagCommandHandler() }
    single<CommandHandler<UpdateProviderCommand, Unit>>(named("updateProvider")) { UpdateProviderCommandHandler() }
    single<CommandHandler<UpdateProviderImageCommand, Unit>>(named("updateProviderImage")) { UpdateProviderImageCommandHandler(get()) }
    single<CommandHandler<CreateCollectionCommand, CreateCollectionResponse>>(named("createCollection")) { CreateCollectionCommandHandler() }
    single<CommandHandler<UpdateCollectionCommand, Unit>>(named("updateCollection")) { UpdateCollectionCommandHandler() }
    single<CommandHandler<UpdateCollectionGamesCommand, Unit>>(named("updateCollectionGames")) { UpdateCollectionGamesCommandHandler() }
    single<CommandHandler<PlayGameCommand, PlayGameResponse>>(named("playGame")) { PlayGameCommandHandler(get()) }
    single<CommandHandler<SyncAllAggregatorCommand, SyncAllAggregatorResponse>>(named("syncAllAggregators")) { SyncAggregatorCommandHandler(get()) }
}
