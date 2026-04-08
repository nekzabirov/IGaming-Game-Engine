package infrastructure.koin

import application.Bus
import application.ICommand
import application.ICommandHandler
import application.IQuery
import application.IQueryHandler

/**
 * Direct-map [Bus] implementation. Command/query class → handler maps are built
 * explicitly in [busModule], no reflection or auto-discovery.
 *
 * Command dispatch unwraps the handler's `Result<R>` via `getOrThrow()` so the
 * gRPC exception interceptor can translate domain exceptions to status codes;
 * query dispatch is a direct pass-through.
 */
class BusImpl(
    private val commandHandlers: Map<Class<*>, ICommandHandler<*, *>>,
    private val queryHandlers: Map<Class<*>, IQueryHandler<*, *>>,
) : Bus {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> invoke(command: ICommand<R>): R {
        val handler = commandHandlers[command::class.java] as? ICommandHandler<ICommand<R>, R>
            ?: error("No handler registered for command: ${command::class.simpleName}")
        return handler.handle(command).getOrThrow()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <R> invoke(query: IQuery<R>): R {
        val handler = queryHandlers[query::class.java] as? IQueryHandler<IQuery<R>, R>
            ?: error("No handler registered for query: ${query::class.simpleName}")
        return handler.handle(query)
    }
}
