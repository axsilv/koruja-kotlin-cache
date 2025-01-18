package com.koruja.kotlin.cache.decorators

import com.koruja.cache.core.CacheProperties
import com.koruja.cache.core.Decorator
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue

class LoggingDecorator(
    private val properties: CacheProperties,
) : Decorator {
    companion object {
        val log = KotlinLogging.logger { }
    }

    override suspend fun <T> decorate(
        t: T,
        function: suspend () -> T,
    ): T {
        if (log.isDebugEnabled() || properties.isCacheDebugEnabled) {
            log.info {
                "[koruja cache debug] Handling $t"
            }

            val (execution, duration) =
                measureTimedValue {
                    function()
                }

            log.info {
                "[koruja cache debug] Handled $t with duration $duration"
            }

            return execution
        }

        return function()
    }
}
