package com.koruja.cache.core

interface Decorator {
    suspend fun <T> decorate(function: suspend () -> T): T
}
