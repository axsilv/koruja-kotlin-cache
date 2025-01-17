package com.koruja.cache.core

interface Decorator {
    suspend fun <T> decorate(
        t: T,
        function: suspend () -> T,
    ): T
}
