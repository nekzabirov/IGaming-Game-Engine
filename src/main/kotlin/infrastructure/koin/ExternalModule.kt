package infrastructure.koin

import application.port.external.IBackgroundTaskPort
import application.port.external.ICurrencyPort
import application.port.external.IEventPublisherPort
import application.port.external.IPlayerLimitPort
import application.port.external.IPlayerPort
import application.port.external.IWalletPort
import application.port.external.IWebhookGuardPort
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import infrastructure.gamehub.GameHubClient
import infrastructure.gamehub.gameHubChannel
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
import org.koin.dsl.module

private val PAM_CHANNEL = named("pam")
private val GAMEHUB_CHANNEL = named("gamehub")

val externalModule = module {
    // One gRPC channel to pam-engine: the player account, the wallet ledger and the currency
    // registry all live there now, so the wallet/currency/profile adapters share it.
    single<ManagedChannel>(PAM_CHANNEL) { pamChannel(get()) }
    single<IWalletPort> { WalletAdapter(channel = get(PAM_CHANNEL)) }
    single<IPlayerLimitPort> { PlayerLimitRedis(config = get()) }
    single<IWebhookGuardPort> { WebhookGuardRedis(config = get()) }
    single<ICurrencyPort> { CurrencyAdapter(channel = get(PAM_CHANNEL)) }
    single<IPlayerPort> { PamAdapter(channel = get(PAM_CHANNEL)) }
    single<IBackgroundTaskPort> { BackgroundWorker() }
    // One RabbitMQ connection, split channels: the publisher owns a dedicated confirm-mode
    // channel (created lazily inside RabbitAppEventPublisher); this Channel single backs the
    // consumers + topology declaration only.
    single<Connection> { rabbitMqConnection(get()) }
    single<Channel> { get<Connection>().createChannel() }
    single<IEventPublisherPort> { RabbitAppEventPublisher(connection = get()) }

    // GameHub — the single upstream for every vendor integration now, casino AND sportsbook.
    single<ManagedChannel>(GAMEHUB_CHANNEL) { gameHubChannel(get()) }
    single { GameHubClient(channel = get(GAMEHUB_CHANNEL), config = get()) }

    single { PlaceSpinEventConsumer(channel = get(), decreasePlayerLimit = get()) }
}
