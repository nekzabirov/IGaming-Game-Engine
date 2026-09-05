package api.grpc.config

import api.grpc.service.CollectionGrpcService
import api.grpc.service.FreespinGrpcService
import api.grpc.service.CasinoGameGrpcService
import api.grpc.service.JackpotGrpcService
import api.grpc.service.WalletGrpcService
import api.grpc.service.CasinoProviderGrpcService
import api.grpc.service.CasinoRoundGrpcService
import api.grpc.service.SportbookGrpcService
import api.grpc.service.WinnerGrpcService
import org.koin.dsl.module

val grpcModule = module {
    single { CasinoGameGrpcService(get()) }
    single { CasinoProviderGrpcService(get()) }
    single { CollectionGrpcService(get()) }
    single { FreespinGrpcService(get()) }
    single { WinnerGrpcService(get()) }
    single { CasinoRoundGrpcService(get()) }
    single { JackpotGrpcService() }
    single { SportbookGrpcService(get()) }
    single { WalletGrpcService(bus = get(), config = get()) }
}
