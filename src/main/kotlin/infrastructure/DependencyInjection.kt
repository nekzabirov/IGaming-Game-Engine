package infrastructure

import application.port.outbound.*
import application.saga.spin.end.EndSpinSaga
import application.saga.spin.place.PlaceSpinSaga
import application.saga.spin.rollback.RollbackSpinSaga
import application.saga.spin.settle.SettleSpinSaga
import application.service.*
import com.nekgamebling.infrastructure.handler.handlerModule
import infrastructure.api.grpc.grpcModule
import infrastructure.external.UnitCurrencyAdapter
import infrastructure.external.s3.S3FileAdapter
import infrastructure.external.turbo.TurboPlayerAdapter
import infrastructure.external.turbo.TurboWalletAdapter
import infrastructure.aggregator.AggregatorModule
import infrastructure.persistence.DBModule
import io.ktor.server.application.*
import org.koin.dsl.module

/**
 * Koin module for dependency injection.
 * All dependencies use constructor injection.
 */
fun coreModule() = module {
    includes(
        DBModule,
        adapterModule,
        serviceModule,
        sagaModule,
        handlerModule,
        grpcModule,
        AggregatorModule,
    )
}

private val adapterModule = module {
    // ==========================================
    // Infrastructure - Ports/Adapters
    // ==========================================
    single<WalletAdapter> { TurboWalletAdapter() }
    single<PlayerAdapter> { TurboPlayerAdapter() }
    single<CurrencyAdapter> { UnitCurrencyAdapter() }
    single<FileAdapter> {
        S3FileAdapter(
            endpoint = System.getenv("S3_ENDPOINT") ?: "http://localhost:9000",
            accessKey = System.getenv("S3_ACCESS_KEY") ?: "minioadmin",
            secretKey = System.getenv("S3_SECRET_KEY") ?: "minioadmin",
            bucketName = System.getenv("S3_BUCKET") ?: "uploads",
            region = System.getenv("S3_REGION") ?: "us-east-1"
        )
    }
}

private val serviceModule = module {
    // ==========================================
    // Application Services
    // ==========================================
    single { GameService(get(), get(), get()) }
    single { SessionService(get(), get(), get(), get(), get()) }
    single { SpinService(get(), get(), get(), get()) }
    single { AggregatorService(get(), get()) }
    single { FreespinService(get(), get()) }
    single { GameSyncService(get(), get(), get(), get()) }
}

private val sagaModule = module {
    // ==========================================
    // Application Sagas - Distributed Transactions
    // ==========================================
    factory { PlaceSpinSaga(get(), get(), get(), get(), get(), get(), get()) }
    factory { SettleSpinSaga(get(), get(), get(), get(), get()) }
    factory { EndSpinSaga(get(), get(), get()) }
    factory { RollbackSpinSaga(get(), get(), get(), get(), get()) }
}

/**
 * Extension to get the core module for an Application.
 */
val Application.gameCoreModule get() = coreModule()
