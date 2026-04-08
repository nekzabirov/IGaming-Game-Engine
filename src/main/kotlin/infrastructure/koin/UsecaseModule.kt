package infrastructure.koin

import application.usecase.FinishRoundUsecase
import application.usecase.OpenSessionUsecase
import application.usecase.ProcessSpinUsecase
import application.usecase.SyncAggregatorUsecase
import org.koin.dsl.module

val usecaseModule = module {
    single {
        ProcessSpinUsecase(
            spinRepository = get(),
            eventPort = get(),
            walletPort = get(),
            playerLimitPort = get(),
            backgroundTaskPort = get(),
        )
    }
    single {
        OpenSessionUsecase(
            aggregatorFactory = get(),
            sessionRepository = get(),
            eventPort = get(),
        )
    }
    single {
        FinishRoundUsecase(
            roundRepository = get(),
            eventPort = get(),
        )
    }
    single {
        SyncAggregatorUsecase(
            aggregatorFactory = get(),
            gameRepository = get(),
            gameVariantRepository = get(),
            providerRepository = get(),
        )
    }
}
