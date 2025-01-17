package com.koruja.cache.inmemory

import com.koruja.cache.core.Cache
import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import com.koruja.cache.core.CacheException.CacheAlreadyPersisted
import com.koruja.cache.core.CacheProperties
import com.koruja.cache.core.Decorator
import com.koruja.kotlin.cache.decorators.LoggingDecorator
import com.koruja.kotlin.cache.decorators.WithTimeoutDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class InMemoryCache(
    private val properties: CacheProperties,
    private val expirationDecider: InMemoryExpirationDecider = InMemoryExpirationDeciderGeneric(),
    private val insertDecorators: List<Decorator> =
        listOf(
            WithTimeoutDecorator(timeoutMillis = 400),
            LoggingDecorator(properties = properties),
        ),
    private val selectDecorators: List<Decorator> =
        listOf(
            WithTimeoutDecorator(timeoutMillis = 1500),
            LoggingDecorator(properties = properties),
        ),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val cache: ConcurrentHashMap<CacheEntryKey, CacheEntry> = ConcurrentHashMap(),
    private val cacheExpirations: ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntryKey>> = ConcurrentHashMap(),
) : Cache {
    private val mutex: Mutex = Mutex()

    init {
        scope.launch { expirationWorker() }
    }

    override suspend fun insert(entry: CacheEntry) =
        runCatching {
            val functionToDecorate =
                suspend {
                    select(key = entry.key).let { result ->
                        result.getOrNull()?.let {
                            throw CacheAlreadyPersisted()
                        }

                        result.exceptionOrNull()?.let {
                            throw it
                        }
                    }

                    addCacheExpirations(
                        key = entry.key,
                        expiresAt = entry.expiresAt,
                    )

                    cache[entry.key] = entry
                }

            val decoratedFunction =
                insertDecorators.fold(functionToDecorate) { acc, decorator ->
                    { decorator.decorate(t = entry, function = acc) }
                }

            decoratedFunction()
        }

    override suspend fun insertAsync(entries: List<CacheEntry>) =
        supervisorScope {
            runCatching {
                entries
                    .map { entry ->
                        async {
                            insert(entry = entry)
                        }
                    }.awaitAll()
                Unit
            }
        }

    override suspend fun launchInsert(entry: CacheEntry) =
        supervisorScope {
            runCatching {
                launch {
                    insert(entry = entry)
                }
                Unit
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

    override suspend fun selectAsync(key: CacheEntryKey) =
        supervisorScope {
            runCatching {
                scope
                    .async {
                        cache[key]?.let { entry ->
                            if (entry.expiresAt >= Clock.System.now()) {
                                return@async entry
                            }
                        }

                        return@async null
                    }.await()
            }
        }

    override suspend fun selectAllAsync() =
        supervisorScope {
            runCatching {
                async {
                    cache.values.toList()
                }.await()
            }
        }

    override suspend fun cleanAll() =
        supervisorScope {
            runCatching {
                cache
                    .keys()
                    .toList()
                    .map { key ->
                        scope.launch {
                            cache.remove(key)
                        }
                    }.joinAll()

                cacheExpirations
                    .keys()
                    .toList()
                    .map { key ->
                        scope.launch {
                            cacheExpirations.remove(key)
                        }
                    }.joinAll()
            }
        }

    private suspend fun expirationWorker() {
        while (true) {
            supervisorScope {
                runCatching {
                    cacheExpirations
                        .toList()
                        .map { (expiresAt, keys) ->
                            launch {
                                deleteIfExpired(keys = keys, expiresAt = expiresAt)
                            }
                        }.joinAll()
                }
            }
        }
    }

    private suspend fun deleteIfExpired(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant,
    ) = supervisorScope {
        runCatching {
            async {
                val shouldRemove =
                    expirationDecider.shouldRemove(
                        keys = keys,
                        expiresAt = expiresAt,
                    )

                if (shouldRemove) {
                    delete(keys = keys, expiresAt = expiresAt)
                }
            }.await()
        }
    }

    private suspend fun delete(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant,
    ) = supervisorScope {
        runCatching {
            keys
                .map {
                    launch { cache.remove(it) }
                }.toList()
                .joinAll()

            cacheExpirations.remove(expiresAt)
        }
    }
}
