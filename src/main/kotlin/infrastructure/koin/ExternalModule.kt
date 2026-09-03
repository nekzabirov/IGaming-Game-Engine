package infrastructure.koin

import application.port.external.IBackgroundTaskPort
import application.port.external.ICurrencyPort
import application.port.external.IEventPublisherPort
import application.port.external.IPlayerLimitPort
import application.port.external.IPlayerPort
import application.port.external.IWalletPort
import application.port.external.IWebhookGuardPort
import application.port.factory.AggregatorAdapterProvider
import application.port.factory.IAggregatorFactory
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import infrastructure.aggregator.AggregatorRegistry
import infrastructure.aggregator.gambutsoft.GambutsoftAdapterProvider
import infrastructure.aggregator.gamehub.GameHubAdapterProvider
import infrastructure.aggregator.gamingflow.GamingFlowAdapterProvider
import infrastructure.aggregator.onegamehub.OneGameHubAdapterProvider
import infrastructure.aggregator.pateplay.PateplayAdapterProvider
import infrastructure.aggregator.pragmatic.PragmaticAdapterProvider
import infrastructure.aggregator.skyline.SkylineAdapterProvider
import infrastructure.aggregator.tech01sport.Tech01SportAdapterProvider
import infrastructure.aggregator.tongame.TongameAdapterProvider
import infrastructure.pam.CurrencyAdapter
import infrastructure.pam.PamAdapter
import infrastructure.pam.WalletAdapter
import infrastructure.pam.pamChannel
import infrastructure.rabbitmq.PlaceSpinEventConsumer
import infrastructure.rabbitmq.RabbitAppEventPublisher
import infrastructure.rabbitmq.rabbitMqConnection
import infrastructure.redis.PlayerLimitRedis
import infrastructure.redis.WebhookGuardRedis
import infrastructure.util.BackgroundWorker
import io.grpc.ManagedChannel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val externalModule = module {
    // One gRPC channel to pam-engine: the player account, the wallet ledger and the currency
    // registry all live there now, so the wallet/currency/profile adapters share it.
    single<ManagedChannel> { pamChannel(get()) }
    single<IWalletPort> { WalletAdapter(channel = get()) }
    single<IPlayerLimitPort> { PlayerLimitRedis(config = get()) }
    single<IWebhookGuardPort> { WebhookGuardRedis(config = get()) }
    single<ICurrencyPort> { CurrencyAdapter(channel = get()) }
    single<IPlayerPort> { PamAdapter(channel = get()) }
    single<IBackgroundTaskPort> { BackgroundWorker() }
    // One RabbitMQ connection, split channels: the publisher owns a dedicated confirm-mode
    // channel (created lazily inside RabbitAppEventPublisher); this Channel single backs the
    // consumers + topology declaration only.
    single<Connection> { rabbitMqConnection(get()) }
    single<Channel> { get<Connection>().createChannel() }
    single<IEventPublisherPort> { RabbitAppEventPublisher(connection = get()) }

    // Aggregator providers — add a new aggregator by binding another AggregatorAdapterProvider.
    single(named("onegamehub")) { OneGameHubAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("pragmatic")) { PragmaticAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("pateplay")) { PateplayAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("tongame")) { TongameAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("gamingflow")) { GamingFlowAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("tech01sport")) { Tech01SportAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("gambutsoft")) { GambutsoftAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("skyline")) { SkylineAdapterProvider() } bind AggregatorAdapterProvider::class
    single(named("gamehub")) { GameHubAdapterProvider() } bind AggregatorAdapterProvider::class
    single<IAggregatorFactory> {
        AggregatorRegistry(providers = getAll<AggregatorAdapterProvider>())
    }

    single { PlaceSpinEventConsumer(channel = get(), decreasePlayerLimit = get()) }
}
