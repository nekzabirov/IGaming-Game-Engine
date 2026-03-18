package infrastructure.koin

import application.port.storage.IAggregatorRepository
import application.port.storage.ICollectionRepository
import application.port.storage.IGameRepository
import application.port.storage.IGameVariantRepository
import application.port.storage.IProviderRepository
import application.port.storage.IRoundRepository
import application.port.storage.ISessionRepository
import application.port.storage.ISpinRepository
import infrastructure.persistence.repository.AggregatorRepositoryImpl
import infrastructure.persistence.repository.CollectionRepositoryImpl
import infrastructure.persistence.repository.GameRepositoryImpl
import infrastructure.persistence.repository.GameVariantRepositoryImpl
import infrastructure.persistence.repository.ProviderRepositoryImpl
import infrastructure.persistence.repository.RoundRepositoryImpl
import infrastructure.persistence.repository.SessionRepositoryImpl
import infrastructure.persistence.repository.SpinRepositoryImpl
import org.koin.dsl.module

val persistenceModule = module {
    single<ISessionRepository> { SessionRepositoryImpl() }
    single<IRoundRepository> { RoundRepositoryImpl() }
    single<ISpinRepository> { SpinRepositoryImpl() }
    single<IGameRepository> { GameRepositoryImpl() }
    single<IGameVariantRepository> { GameVariantRepositoryImpl() }
    single<IProviderRepository> { ProviderRepositoryImpl() }
    single<ICollectionRepository> { CollectionRepositoryImpl() }
    single<IAggregatorRepository> { AggregatorRepositoryImpl() }
}
