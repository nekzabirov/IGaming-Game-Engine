package infrastructure.koin

import domain.repository.IAggregatorRepository
import domain.repository.ICollectionRepository
import domain.repository.IGameRepository
import domain.repository.IGameVariantRepository
import domain.repository.IProviderRepository
import domain.repository.IRoundRepository
import domain.repository.ISessionRepository
import domain.repository.ISpinRepository
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
