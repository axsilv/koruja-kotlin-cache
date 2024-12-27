package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class InMemoryCache : Cache {
    private val cache: ConcurrentHashMap<CacheEntryKey, CacheEntry> = ConcurrentHashMap()
    private val cacheExpirations: ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntryKey>> = ConcurrentHashMap()
    private val defaultDuration: Duration = 60.seconds
    private val mutex: Mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch { expirationWorker() }
    }

    override fun insert(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Result<Unit> =
        runCatching {
            addCacheExpirations(
                key = entry.id,
                expiresAt = expiresAt,
            )

            cache[entry.id] = entry
        }

    override fun insertAsync(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Deferred<Result<Unit>> =
        scope.async {
            runCatching {
                addCacheExpirations(
                    key = entry.id,
                    expiresAt = expiresAt,
                )

                cache[entry.id] = entry
            }
        }

    override fun launchInsert(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Job =
        scope.launch {
            runCatching {
                addCacheExpirations(
                    key = entry.id,
                    expiresAt = expiresAt,
                )

                cache[entry.id] = entry
            }
        }

    private fun addCacheExpirations(
        key: CacheEntryKey,
        expiresAt: Instant,
    ) = cacheExpirations[expiresAt]?.add(key)

    override fun select(key: CacheEntryKey): Result<CacheEntry?> =
        runCatching {
            cache.get(key = key)?.let { entry ->
                if (entry.expiresAt >= Clock.System.now()) {
                    return@runCatching entry
                }
            }

            return@runCatching null
        }

    override fun selectAll(): Result<List<CacheEntry>> = runCatching { cache.values.toList() }

    private suspend fun expirationWorker() {
        cacheExpirations.forEach { (expiresAt, keys) ->
            if (expiresAt <= Clock.System.now()) {
                mutex.withLock {
                    keys.forEach { cache.remove(it) }

                    cacheExpirations.remove(expiresAt)
                }
            }
        }

        delay(1.seconds)

        expirationWorker()
    }
}
