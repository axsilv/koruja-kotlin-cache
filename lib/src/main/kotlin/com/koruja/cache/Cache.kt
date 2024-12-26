package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryKey
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Cache {
    private val cache: ConcurrentHashMap<CacheEntryKey, CacheEntry> = ConcurrentHashMap()
    private val cacheExpirations: ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntryKey>> = ConcurrentHashMap()
    private val defaultDuration: Duration = 60.seconds
    private val mutex: Mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch { expirationWorker() }
    }

    fun insert(
        entry: CacheEntry,
        expiresAt: Instant = Clock.System.now().plus(defaultDuration)
    ): Result<Unit> = runCatching {
        addCacheExpirations(
            key = entry.id,
            expiresAt = expiresAt
        )

        cache[entry.id] = entry
    }

    private fun addCacheExpirations(
        key: CacheEntryKey,
        expiresAt: Instant
    ) = cacheExpirations[expiresAt]?.add(key)

    fun select(key: CacheEntryKey): Result<CacheEntry?> = runCatching { cache.get(key = key) }

    private suspend fun expirationWorker() {
        runCatching {
            mutex.withLock {
                cacheExpirations.forEach { (expiresAt, keys) ->
                    if (expiresAt <= Clock.System.now()) {
                        keys.forEach { cache.remove(it) }

                        cacheExpirations.remove(expiresAt)
                    }
                }

                delay(1.minutes)

                expirationWorker()
            }
        }
    }
}
