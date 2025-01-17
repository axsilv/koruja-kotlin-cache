package com.koruja.cache.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MockedCache : Cache {
    private val caches = mutableListOf<CacheEntry>()

    override suspend fun insert(entry: CacheEntry) =
        runCatching {
            caches.add(entry)
            Unit
        }

    override suspend fun insertAsync(entries: List<CacheEntry>) =
        runCatching {
            CoroutineScope(Dispatchers.IO).async { caches.addAll(entries) }.await()
            Unit
        }

    override suspend fun launchInsert(entry: CacheEntry) =
        runCatching {
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
