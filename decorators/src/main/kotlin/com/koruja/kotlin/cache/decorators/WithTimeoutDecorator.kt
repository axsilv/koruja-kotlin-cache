package com.koruja.kotlin.cache.decorators

import com.koruja.cache.core.Decorator
import kotlinx.coroutines.withTimeout

class WithTimeoutDecorator(
    private val timeoutMillis: Long,
) : Decorator {
    override suspend fun <T> decorate(
        t: T,
        function: suspend () -> T,
    ): T =
        withTimeout(timeoutMillis) {
            function()
        }
}
