package com.koruja.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SingleCache {
    private var cache: Cache? = null
    private var started: Boolean = false
    private val mutex: Mutex = Mutex()

    suspend fun insert(cache: Cache) = mutex.withLock {
        if (started.not()) {
            this.cache = cache
            this.started = true
        }
    }

    fun select(): Cache? = cache

    fun isStarted(): Boolean = started
}