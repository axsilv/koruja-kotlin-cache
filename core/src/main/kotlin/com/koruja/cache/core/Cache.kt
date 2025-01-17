package com.koruja.cache.core

interface Cache {
    suspend fun insert(entry: CacheEntry): Result<Unit>

    suspend fun insertAsync(entries: List<CacheEntry>): Result<Unit>

    suspend fun launchInsert(entry: CacheEntry): Result<Unit>

    suspend fun select(key: CacheEntry.CacheEntryKey): Result<CacheEntry?>

    suspend fun selectAll(): Result<List<CacheEntry>>

    suspend fun selectAsync(key: CacheEntry.CacheEntryKey): Result<CacheEntry?>

    suspend fun selectAllAsync(): Result<List<CacheEntry>>

    suspend fun cleanAll(): Result<Unit>
}
