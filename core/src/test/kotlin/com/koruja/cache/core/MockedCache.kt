package com.koruja.cache.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

class MockedCache : Cache {
    private val caches = mutableListOf<CacheEntry>()

    override suspend fun insert(entry: CacheEntry, expiresAt: Instant) {
        caches.add(entry)
    }

    override suspend fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit> =
        CoroutineScope(Dispatchers.Default).async { caches.add(entry) }

    override suspend fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job =
        CoroutineScope(Dispatchers.Default).launch { caches.add(entry) }


    override suspend fun select(key: CacheEntry.CacheEntryKey): CacheEntry? = caches.find { it.key == key }

    override suspend fun selectAll(): List<CacheEntry> = caches

    override suspend fun selectAsync(key: CacheEntry.CacheEntryKey): Deferred<CacheEntry?> =
        CoroutineScope(Dispatchers.Default).async { caches.find { it.key == key } }

    override suspend fun selectAllAsync(): Deferred<List<CacheEntry>> =
        CoroutineScope(Dispatchers.Default).async { caches }

    override suspend fun cleanAll(): Job =
        CoroutineScope(Dispatchers.Default).launch { caches.forEach { caches.remove(it) } }
}