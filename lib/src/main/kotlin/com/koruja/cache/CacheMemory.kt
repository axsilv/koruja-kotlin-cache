package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryId
import java.util.concurrent.ConcurrentHashMap

class CacheMemory {
    private val cache: ConcurrentHashMap<CacheKey, ConcurrentHashMap<CacheEntryId, CacheEntry>> = ConcurrentHashMap()
}