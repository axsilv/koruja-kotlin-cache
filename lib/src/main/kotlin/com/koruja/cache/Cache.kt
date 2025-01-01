package com.koruja.cache

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.datetime.Instant

interface Cache {
    suspend fun insert(entry: CacheEntry, expiresAt: Instant)
    fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit>
    fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job
    fun select(key: CacheEntry.CacheEntryKey): CacheEntry?
    fun selectAll(): List<CacheEntry>
    fun selectAsync(key: CacheEntry.CacheEntryKey): Deferred<CacheEntry?>
    suspend fun selectAllAsync(): Deferred<List<CacheEntry>>
}
