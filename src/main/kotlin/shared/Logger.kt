package shared

import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis

internal object Logger {
    private val logger = LoggerFactory.getLogger("App")!!

    fun info(msg: String) = logger.info(msg)

    fun info(format: String, vararg arguments: Any) = logger.info(format, *arguments)

    fun warn(msg: String) = logger.warn(msg)

    fun warn(format: String, vararg arguments: Any) = logger.warn(format, *arguments)

    fun error(msg: String) = logger.error(msg)

    fun error(msg: String, throwable: Throwable) = logger.error(msg, throwable)

    fun error(format: String, vararg arguments: Any) = logger.error(format, *arguments)

    inline fun <T> profile(operation: String, block: () -> T): T {
        var result: T
        val timeMs = measureTimeMillis {
            result = block()
        }
        logger.info("[PROFILE] {} completed in {} ms", operation, timeMs)
        return result
    }

    suspend inline fun <T> profileSuspend(operation: String, crossinline block: suspend () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val timeMs = System.currentTimeMillis() - startTime
        logger.info("[PROFILE] {} completed in {} ms", operation, timeMs)
        return result
    }
}