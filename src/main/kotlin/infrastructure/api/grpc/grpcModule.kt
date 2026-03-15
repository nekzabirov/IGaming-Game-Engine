package infrastructure.api.grpc

import infrastructure.api.grpc.service.AggregatorGrpcService
import infrastructure.api.grpc.service.CollectionGrpcService
import infrastructure.api.grpc.service.RoundGrpcService
import infrastructure.api.grpc.service.GameGrpcService
import infrastructure.api.grpc.service.ProviderGrpcService
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for gRPC services.
 */
val grpcModule = module {
    // ==========================================
    // gRPC Services
    // ==========================================
    single {
        GameGrpcService(
            findGameQueryHandler = get(named("findGame")),
            findAllGameQueryHandler = get(named("findAllGames")),
            gameDemoUrlQueryHandler = get(named("gameDemoUrl")),
            playGameCommandHandler = get(named("playGame")),
            updateGameCommandHandler = get(named("updateGame")),
            updateGameImageCommandHandler = get(named("updateGameImage")),
            addGameTagCommandHandler = get(named("addGameTag")),
            removeGameTagCommandHandler = get(named("removeGameTag")),
            addFavouriteGameCommandHandler = get(named("addFavouriteGame")),
            removeFavouriteGameCommandHandler = get(named("removeFavouriteGame")),
            findAllGameWinsQueryHandler = get(named("findAllGameWins")),
            freespinService = get()
        )
    }

    single {
        ProviderGrpcService(
            findProviderQueryHandler = get(named("findProvider")),
            findAllProvidersQueryHandler = get(named("findAllProviders")),
            updateProviderCommandHandler = get(named("updateProvider")),
            updateProviderImageCommandHandler = get(named("updateProviderImage"))
        )
    }

    single {
        CollectionGrpcService(
            createCollectionCommandHandler = get(named("createCollection")),
            findCollectionQueryHandler = get(named("findCollection")),
            findAllCollectionsQueryHandler = get(named("findAllCollections")),
            updateCollectionCommandHandler = get(named("updateCollection")),
            updateCollectionGamesCommandHandler = get(named("updateCollectionGames"))
        )
    }

    single {
        RoundGrpcService(
            findRoundQueryHandler = get(named("findRound")),
            findAllRoundQueryHandler = get(named("findAllRounds"))
        )
    }

    single {
        AggregatorGrpcService(
            createAggregatorCommandHandler = get(named("createAggregator")),
            findAggregatorQueryHandler = get(named("findAggregator")),
            findAllAggregatorQueryHandler = get(named("findAllAggregators")),
            updateAggregatorCommandHandler = get(named("updateAggregator"))
        )
    }
}

