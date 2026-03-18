package application.cqrs

interface ICommand<R>

interface ICommandHandler<C : ICommand<R>, R> {
    suspend fun handle(command: C): Result<R>
}

interface IQuery<R>

interface IQueryHandler<Q : IQuery<R>, R> {
    suspend fun handle(query: Q): R
}
