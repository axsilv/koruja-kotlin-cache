package com.koruja.kotlin.cache.decorators

import com.koruja.cache.core.Decorator
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue

class LoggingDecorator(
    private val log: KLogger = KotlinLogging.logger { LoggingDecorator::class.java },
) : Decorator {
    override suspend fun <T> decorate(
        t: T,
        function: suspend () -> T,
    ): T {
        if (log.isDebugEnabled()) {
            log.debug {
                "Handling $t"
            }

            val (execution, duration) =
                measureTimedValue {
                    function()
                }

            log.debug {
                "Handled $t with duration $duration"
            }

            return execution
        }

        return function()
    }
}
