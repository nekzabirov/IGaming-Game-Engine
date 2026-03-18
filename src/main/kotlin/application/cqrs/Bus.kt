package application.cqrs

interface Bus {
    suspend operator fun <R> invoke(query: IQuery<R>) : R

    suspend operator fun <R> invoke(command: ICommand<R>) : R
}