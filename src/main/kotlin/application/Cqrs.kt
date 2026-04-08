package application

/**
 * CQRS dispatcher contracts.
 *
 * - [ICommand]/[ICommandHandler] — write side; handlers return `Result<R>` so the
 *   bus + gRPC interceptor can translate domain exceptions to status codes.
 * - [IQuery]/[IQueryHandler] — read side; handlers return `R` directly.
 * - [Bus] — single dispatch entry point used by gRPC services and webhooks.
 *
 * Handlers are wired in `infrastructure/koin/HandlerModule.kt` and routed by
 * `infrastructure/koin/BusModule.kt`'s explicit class-to-handler maps.
 */
interface Bus {

    suspend operator fun <R> invoke(query: IQuery<R>): R

    suspend operator fun <R> invoke(command: ICommand<R>): R
}

interface ICommand<R>

interface ICommandHandler<C : ICommand<R>, R> {

    suspend fun handle(command: C): Result<R>
}

interface IQuery<R>

interface IQueryHandler<Q : IQuery<R>, R> {

    suspend fun handle(query: Q): R
}
