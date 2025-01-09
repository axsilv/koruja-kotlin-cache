package com.koruja.cache.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.datetime.Instant

interface Cache {
    suspend fun insert(entry: CacheEntry, expiresAt: Instant)
    suspend fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit>
    suspend fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job
    suspend fun select(key: CacheEntry.CacheEntryKey): CacheEntry?
    suspend fun selectAll(): List<CacheEntry>
    suspend fun selectAsync(key: CacheEntry.CacheEntryKey): Deferred<CacheEntry?>
    suspend fun selectAllAsync(): Deferred<List<CacheEntry>>
    suspend fun cleanAll(): Job
}
