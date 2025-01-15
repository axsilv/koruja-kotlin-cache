package com.koruja.cache.inmemory

import com.koruja.cache.core.Cache
import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import com.koruja.cache.core.CacheException.CacheAlreadyPersisted
import com.koruja.cache.core.Decorator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch { expirationWorker() }
    }

    override suspend fun insert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        select(key = entry.key).let { result ->
            if (result.isSuccess && result.getOrNull() != null) {
                throw CacheAlreadyPersisted()
            }
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
    ) = coroutineScope {
        return@coroutineScope runCatching {
            async {
                selectAsync(key = entry.key).let { result ->
                    if (result.isSuccess && result.getOrNull() != null) {
                        throw CacheAlreadyPersisted()
                    }
                }

                addCacheExpirations(
                    key = entry.key,
                    expiresAt = expiresAt,
                )

                cache[entry.key] = entry
            }.await()
        }
    }


    override suspend fun launchInsert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = coroutineScope {
        runCatching {
            launch {
                selectAsync(key = entry.key).let { result ->
                    if (result.isSuccess && result.getOrNull() != null) {
                        throw CacheAlreadyPersisted()
                    }

                    val exceptionOrNull = result.exceptionOrNull()
                    if (result.isFailure && exceptionOrNull != null) {
                        throw exceptionOrNull
                    }
                }

                addCacheExpirations(
                    key = entry.key,
                    expiresAt = expiresAt,
                )

                cache[entry.key] = entry
            }.join()
        }
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

    override suspend fun select(key: CacheEntryKey): Result<CacheEntry?> {
        return runCatching {
            cache[key]?.let { entry ->
                if (entry.expiresAt >= Clock.System.now()) {
                    return@runCatching entry
                }
            }
            null
        }.let {
            if (it.isSuccess) {
                Result.success(it.getOrNull())
            } else {
                Result.failure(it.exceptionOrNull() ?: RuntimeException("Unknown error"))
            }
        }
    }


    override suspend fun selectAll() = runCatching { cache.values.toList() }

    override suspend fun selectAsync(key: CacheEntryKey) = coroutineScope {
        runCatching {
            scope.async {
                cache[key]?.let { entry ->
                    if (entry.expiresAt >= Clock.System.now()) {
                        return@async entry
                    }
                }

                return@async null
            }.await()
        }
    }

    override suspend fun selectAllAsync() = coroutineScope {
        runCatching {
            async {
                cache.values.toList()
            }.await()
        }
    }


    override suspend fun cleanAll() = coroutineScope {
        runCatching {
            async {
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
            }.await()
        }
    }

    private suspend fun expirationWorker() {
        while (true) {
            runCatching {
                coroutineScope {
                    cacheExpirations.toList()
                        .map { (expiresAt, keys) ->
                            launch {
                                deleteIfExpired(keys = keys, expiresAt = expiresAt)
                            }
                        }.toList()
                        .joinAll()
                }
            }
        }
    }

    private suspend fun deleteIfExpired(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant
    ) = coroutineScope {
        runCatching {
            async {
                val shouldRemove = expirationDecider.shouldRemove(
                    keys = keys,
                    expiresAt = expiresAt
                )

                if (shouldRemove) {
                    delete(keys = keys, expiresAt = expiresAt)
                }
            }.await()
        }
    }

    private suspend fun delete(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant
    ) = supervisorScope {
        runCatching {
            keys.map {
                launch { cache.remove(it) }
            }.toList()
                .joinAll()

            cacheExpirations.remove(expiresAt)
        }
    }
}
