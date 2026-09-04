package infrastructure.koin

import domain.repository.ICollectionRepository
import domain.repository.ICasinoGameRepository
import domain.repository.ICasinoProviderRepository
import domain.repository.ICasinoRoundRepository
import domain.repository.ISpinRepository
import infrastructure.persistence.repository.CollectionRepositoryImpl
import infrastructure.persistence.repository.CasinoGameRepositoryImpl
import infrastructure.persistence.repository.CasinoProviderRepositoryImpl
import infrastructure.persistence.repository.CasinoRoundRepositoryImpl
import infrastructure.persistence.repository.SpinRepositoryImpl
import org.koin.dsl.module

val persistenceModule = module {
    single<ICasinoRoundRepository> { CasinoRoundRepositoryImpl() }
    single<ISpinRepository> { SpinRepositoryImpl() }
    single<ICasinoGameRepository> { CasinoGameRepositoryImpl() }
    single<ICasinoProviderRepository> { CasinoProviderRepositoryImpl() }
    single<ICollectionRepository> { CollectionRepositoryImpl() }
}
