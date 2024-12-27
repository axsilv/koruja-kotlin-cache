package com.koruja.cache

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.datetime.Instant

interface Cache {
    fun insert(entry: CacheEntry, expiresAt: Instant): Result<Unit>
    fun insertAsync(entry: CacheEntry, expiresAt: Instant): Deferred<Unit>
    fun launchInsert(entry: CacheEntry, expiresAt: Instant): Job
    fun select(key: CacheEntry.CacheEntryKey): Result<CacheEntry?>
    fun selectAll(): Result<List<CacheEntry>>
    fun selectAsync(key: CacheEntry.CacheEntryKey): Deferred<Result<CacheEntry?>>
}
