package com.koruja.cache.core

import kotlinx.datetime.Instant

interface Cache {
    suspend fun insert(entry: CacheEntry, expiresAt: Instant): Result<Unit>
    suspend fun insertAsync(entry: CacheEntry, expiresAt: Instant): Result<Unit>
    suspend fun launchInsert(entry: CacheEntry, expiresAt: Instant): Result<Unit>
    suspend fun select(key: CacheEntry.CacheEntryKey): Result<CacheEntry?>
    suspend fun selectAll(): Result<List<CacheEntry>>
    suspend fun selectAsync(key: CacheEntry.CacheEntryKey): Result<CacheEntry?>
    suspend fun selectAllAsync(): Result<List<CacheEntry>>
    suspend fun cleanAll(): Result<Unit>
}
