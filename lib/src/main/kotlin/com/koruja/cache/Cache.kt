package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheExpirationOperationType.ADD
import com.koruja.cache.CacheExpirationOperationType.REMOVE_WORKER
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Cache {
    private val cache: ConcurrentHashMap<CacheEntryKey, CacheEntry> = ConcurrentHashMap()
    private val cacheExpirations: ConcurrentHashMap<Instant, Set<CacheEntryKey>> = ConcurrentHashMap()
    private val defaultDuration: Duration = 60.seconds
    private val mutex: Mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch { expirationWorker() }
    }

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
        key: CacheEntryKey? = null,
        expiresAt: Instant? = null
    ) = mutex.withLock {
        when (operationType) {
            ADD -> {
                if (key != null && expiresAt != null) {
                    cacheExpirations[expiresAt] = cacheExpirations[expiresAt]?.plus(key) ?: setOf(key)
                }
            }

            REMOVE_WORKER -> {
                cacheExpirations.forEach { (expiresAt, keys) ->
                    if (expiresAt <= Clock.System.now()) {
                        keys.forEach { cache.remove(it) }

                        cacheExpirations.remove(expiresAt)
                    }
                }
            }
        }
    }

    fun select(key: CacheEntryKey): Result<CacheEntry?> = runCatching { cache.get(key = key) }

    private suspend fun expirationWorker() {
        runCatching {
            cacheExpirationCore(
                operationType = REMOVE_WORKER
            )

            delay(1.minutes)

            expirationWorker()
        }
    }
}
