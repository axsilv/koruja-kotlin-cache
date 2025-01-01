package com.koruja.cache.inmemory

import com.koruja.cache.Cache
import com.koruja.cache.CacheEntry
import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheException.CacheAlreadyPersisted
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class InMemoryCache : Cache {
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
        select(key = entry.id)?.let {
            throw CacheAlreadyPersisted()
        }

        addCacheExpirations(
            key = entry.id,
            expiresAt = expiresAt,
        )

        cache[entry.id] = entry
    }

    override fun insertAsync(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Deferred<Unit> = scope.async {
        selectAsync(key = entry.id).await()?.let {
            throw CacheAlreadyPersisted()
        }

        addCacheExpirations(
            key = entry.id,
            expiresAt = expiresAt,
        )

        cache[entry.id] = entry
    }


    override fun launchInsert(
        entry: CacheEntry,
        expiresAt: Instant,
    ): Job = scope.launch {
        selectAsync(key = entry.id).await()?.let {
            throw CacheAlreadyPersisted()
        }

        addCacheExpirations(
            key = entry.id,
            expiresAt = expiresAt,
        )

        cache[entry.id] = entry
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

    override fun select(key: CacheEntryKey): CacheEntry? {
        cache.get(key = key)?.let { entry ->
            if (entry.expiresAt >= Clock.System.now()) {
                return entry
            }
        }

        return null
    }

    override fun selectAll(): List<CacheEntry> = cache.values.toList()

    override fun selectAsync(key: CacheEntryKey): Deferred<CacheEntry?> = scope.async {
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

    private suspend fun expirationWorker() {
        while (true) {
            cacheExpirations.forEach { (expiresAt, keys) ->
                if (expiresAt <= Clock.System.now()) {
                    mutex.withLock {
                        keys.forEach { cache.remove(it) }

                        cacheExpirations.remove(expiresAt)
                    }
                }
            }
        }
    }
}
