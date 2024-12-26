package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheExpirationOperationType.ADD
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ConservativeCache {
    private val cache: ConcurrentHashMap<CacheEntryKey, CacheEntry> = ConcurrentHashMap()
    private val cacheExpirations: ConcurrentHashMap<Instant, Set<CacheEntryKey>> = ConcurrentHashMap()
    private val defaultDuration: Duration = 60.seconds
    private val mutex: Mutex = Mutex()

    suspend fun insert(
        entry: CacheEntry,
        expiresAt: Instant = Clock.System.now().plus(defaultDuration)
    ): Result<Unit> = runCatching {
        cacheExpirationCore(
            operationType = ADD,
            key = entry.id,
            expiresAt = expiresAt
        )

        cache[entry.id] = entry
    }

    private suspend fun cacheExpirationCore(
        operationType: CacheExpirationOperationType,
        key: CacheEntryKey,
        expiresAt: Instant = Clock.System.now().plus(defaultDuration)
    ) = mutex.withLock {
        when (operationType) {
            ADD -> {
                cacheExpirations[expiresAt] = cacheExpirations[expiresAt]?.plus(key) ?: setOf(key)
            }
        }
    }

    fun select(key: CacheEntryKey): Result<CacheEntry?> = runCatching { cache.get(key = key) }
}
