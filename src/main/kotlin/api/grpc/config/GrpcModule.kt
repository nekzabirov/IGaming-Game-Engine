package api.grpc.config

import api.grpc.service.AggregatorGrpcService
import api.grpc.service.CollectionGrpcService
import api.grpc.service.FreespinGrpcService
import api.grpc.service.GameGrpcService
import api.grpc.service.ProviderGrpcService
import api.grpc.service.WinnerGrpcService
import org.koin.dsl.module

val grpcModule = module {
    single { GameGrpcService(get()) }
    single { ProviderGrpcService(get()) }
    single { CollectionGrpcService(get()) }
    single { AggregatorGrpcService(get()) }
    single { FreespinGrpcService(get()) }
    single { WinnerGrpcService(get()) }
}
