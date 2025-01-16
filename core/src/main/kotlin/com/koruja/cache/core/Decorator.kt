package com.koruja.cache.core

interface Decorator {
    suspend fun <T> decorate(execution: suspend () -> T): T
}
