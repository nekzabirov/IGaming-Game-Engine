package infrastructure.koin

import application.usecase.DecreasePlayerLimitUsecase
import application.usecase.FinishCasinoRoundUsecase
import application.usecase.OpenCasinoSessionUsecase
import application.usecase.ProcessSpinUsecase
import application.usecase.SyncCasinoCatalogUsecase
import org.koin.dsl.module

val usecaseModule = module {
    single {
        ProcessSpinUsecase(
            spinRepository = get(),
            eventPublisher = get(),
            walletPort = get(),
            playerLimitPort = get(),
            // On by default: an installation that has nobody else settling free rounds must keep
            // paying them, and a missing variable must not quietly stop paying players.
            freespinToPayout = System.getenv("FREESPIN_TO_PAYOUT")?.toBooleanStrictOrNull() ?: true,
        )
    }
    single {
        OpenCasinoSessionUsecase(
            gameHubClient = get(),
            eventPublisher = get(),
        )
    }
    single {
        FinishCasinoRoundUsecase(
            roundRepository = get(),
            eventPublisher = get(),
        )
    }
    single {
        DecreasePlayerLimitUsecase(
            playerLimitPort = get(),
        )
    }
    single {
        SyncCasinoCatalogUsecase(
            gameHubClient = get(),
            gameRepository = get(),
            providerRepository = get(),
        )
    }
}
