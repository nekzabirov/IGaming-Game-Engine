package infrastructure.koin

import application.cqrs.Bus
import application.cqrs.ICommand
import application.cqrs.ICommandHandler
import application.cqrs.IQuery
import application.cqrs.IQueryHandler

class BusImpl(
    private val commandHandlers: Map<Class<*>, ICommandHandler<*, *>>,
    private val queryHandlers: Map<Class<*>, IQueryHandler<*, *>>
) : Bus {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> invoke(command: ICommand<R>): R {
        val handler = commandHandlers[command::class.java]
            as? ICommandHandler<ICommand<R>, R>
            ?: error("No handler registered for command: ${command::class.simpleName}")

        return handler.handle(command).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> invoke(query: IQuery<R>): R {
        val handler = queryHandlers[query::class.java]
            as? IQueryHandler<IQuery<R>, R>
            ?: error("No handler registered for query: ${query::class.simpleName}")

        return handler.handle(query)
    }
}
