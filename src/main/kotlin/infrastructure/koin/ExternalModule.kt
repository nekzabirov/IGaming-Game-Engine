package infrastructure.koin

import application.port.external.FileAdapter
import application.port.external.IBackgroundTaskPort
import application.port.external.ICurrencyPort
import application.port.external.IEventPort
import application.port.external.IPlayerLimitPort
import application.port.external.IWalletPort
import application.port.factory.IAggregatoryFactory
import infrastructure.aggregator.AggregatorFabricImpl
import infrastructure.aggregator.onegamehub.OneGamehubAdapterFactory
import infrastructure.aggregator.pateplay.PateplayAdapterFactory
import infrastructure.aggregator.pragmatic.PragmaticAdapterFactory
import infrastructure.rabbitmq.PlaceSpinEventConsumer
import infrastructure.rabbitmq.RabbitMqEventPublisher
import infrastructure.redis.PlayerLimitRedis
import infrastructure.s3.S3FileAdapter
import infrastructure.unit.BackgroundWorker
import infrastructure.unit.CurrencyAdapter
import infrastructure.wallet.WalletAdapter
import org.koin.dsl.module

val externalModule = module {
    single<IWalletPort> { WalletAdapter(config = get()) }
    single<FileAdapter> { S3FileAdapter(config = get()) }
    single<IPlayerLimitPort> { PlayerLimitRedis(config = get()) }
    single<ICurrencyPort> { CurrencyAdapter() }
    single<IBackgroundTaskPort> { BackgroundWorker() }
    single<IEventPort> { RabbitMqEventPublisher(application = get(), config = get()) }

    single { OneGamehubAdapterFactory() }
    single { PragmaticAdapterFactory() }
    single { PateplayAdapterFactory() }
    single<IAggregatoryFactory> {
        AggregatorFabricImpl(
            oneGamehubAdapterFactory = get(),
            pragmaticAdapterFactory = get(),
            pateplayAdapterFactory = get(),
        )
    }

    single { PlaceSpinEventConsumer(application = get(), config = get(), playerLimitPort = get()) }
}
