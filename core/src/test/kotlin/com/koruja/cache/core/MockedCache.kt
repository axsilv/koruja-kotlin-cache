package com.koruja.cache.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class MockedCache : Cache {
    private val caches = mutableListOf<CacheEntry>()

    override suspend fun insert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        caches.add(entry)
        Unit
    }

    override suspend fun insertAsync(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        CoroutineScope(Dispatchers.IO).async { caches.add(entry) }.await()
        Unit
    }

    override suspend fun launchInsert(
        entry: CacheEntry,
        expiresAt: Instant,
    ) = runCatching {
        CoroutineScope(Dispatchers.IO).launch { caches.add(entry) }.join()
    }

    override suspend fun select(key: CacheEntry.CacheEntryKey) = runCatching { caches.find { it.key == key } }

    override suspend fun selectAll() = runCatching { caches }

    override suspend fun selectAsync(key: CacheEntry.CacheEntryKey) =
        runCatching {
            CoroutineScope(Dispatchers.IO).async { caches.find { it.key == key } }.await()
        }

    override suspend fun selectAllAsync() =
        runCatching {
            CoroutineScope(Dispatchers.IO).async { caches }.await()
        }

    override suspend fun cleanAll() =
        runCatching {
            CoroutineScope(Dispatchers.IO).launch { caches.forEach { caches.remove(it) } }.join()
        }
}
