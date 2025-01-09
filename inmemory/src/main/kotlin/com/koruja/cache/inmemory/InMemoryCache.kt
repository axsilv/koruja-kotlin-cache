package com.koruja.cache.inmemory

import com.koruja.cache.core.Cache
import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import com.koruja.cache.core.CacheException.CacheAlreadyPersisted
import com.koruja.cache.core.Decorator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class InMemoryCache(
    private val expirationDecider: InMemoryExpirationDecider,
    private val decorators: List<Decorator> = emptyList()
) : Cache {
    private val cache: ConcurrentHashMap<CacheEntryKey, CacheEntry> = ConcurrentHashMap()
    private val cacheExpirations: ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntryKey>> = ConcurrentHashMap()
    private val mutex: Mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        scope.launch { expirationWorker() }
    }

    override suspend fun insert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) {
        select(key = entry.key)?.let {
            throw CacheAlreadyPersisted()
        }

        addCacheExpirations(
            key = entry.key,
            expiresAt = expiresAt,
        )

        cache[entry.key] = entry
    }

    override suspend fun insertAsync(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Deferred<Unit> = scope.async {
        selectAsync(key = entry.key).await()?.let {
            throw CacheAlreadyPersisted()
        }

        addCacheExpirations(
            key = entry.key,
            expiresAt = expiresAt,
        )

        cache[entry.key] = entry
    }


    override suspend fun launchInsert(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Job = scope.launch {
        selectAsync(key = entry.key).await()?.let {
            throw CacheAlreadyPersisted()
        }

        addCacheExpirations(
            key = entry.key,
            expiresAt = expiresAt,
        )

        cache[entry.key] = entry
    }


    private suspend fun addCacheExpirations(
        key: CacheEntryKey,
        expiresAt: Instant,
    ) {
        mutex.withLock {
            val entry = cacheExpirations[expiresAt]

            if (entry == null) {
                cacheExpirations[expiresAt] = ConcurrentLinkedQueue<CacheEntryKey>(listOf(key))
            } else {
                entry.add(key)
            }
        }
    }

    override suspend fun select(key: CacheEntryKey): CacheEntry? {
        cache.get(key = key)?.let { entry ->
            if (entry.expiresAt >= Clock.System.now()) {
                return entry
            }
        }

        return null
    }

    override suspend fun selectAll(): List<CacheEntry> = cache.values.toList()

    override suspend fun selectAsync(key: CacheEntryKey): Deferred<CacheEntry?> = scope.async {
        cache.get(key = key)?.let { entry ->
            if (entry.expiresAt >= Clock.System.now()) {
                return@async entry
            }
        }

        return@async null
    }

    override suspend fun selectAllAsync(): Deferred<List<CacheEntry>> = scope.async {
        cache.values.toList()
    }

    override suspend fun cleanAll(): Job = scope.launch {
        val jobs = mutableListOf<Job>()
        jobs += cache.keys()
            .toList()
            .map { key ->
                scope.launch {
                    cache.remove(key)
                }
            }.toList()

        jobs += cacheExpirations.keys()
            .toList()
            .map { key ->
                scope.launch {
                    cacheExpirations.remove(key)
                }
            }.toList()

        jobs.joinAll()
    }

    private suspend fun expirationWorker() {
        while (true) {
            cacheExpirations.toList()
                .map { (expiresAt, keys) ->
                    deleteIfExpired(keys = keys, expiresAt = expiresAt)
                }.toList()
                .joinAll()
        }
    }

    private fun deleteIfExpired(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant
    ) = scope.launch {
        val shouldRemove = expirationDecider.shouldRemove(
            keys = keys,
            expiresAt = expiresAt
        )

        if (shouldRemove) {
            delete(keys = keys, expiresAt = expiresAt)
        }
    }

    private suspend fun delete(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant
    ) {
        keys.map {
            scope.launch { cache.remove(it) }
        }.toList()
            .joinAll()

        cacheExpirations.remove(expiresAt)
    }
}
