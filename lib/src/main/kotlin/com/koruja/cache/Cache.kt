package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Cache {
    private val cache: ConcurrentHashMap<CacheKey, ConcurrentHashMap<CacheEntryId, CacheEntry>> = ConcurrentHashMap()
    private val garbageCache: ConcurrentHashMap<Instant, ConcurrentHashMap<CacheKey, ConcurrentHashMap<CacheEntryId, Unit>>> =
        ConcurrentHashMap()
    private val defaultDuration: Duration = 60.seconds

    suspend fun insert(
        key: CacheKey,
        entry: CacheEntry,
        expiresAt: Instant? = Clock.System.now().plus(defaultDuration)
    ): Boolean = coroutineScope {
        runCatching {
            garbageCache[expiresAt]?.get(key)?.put(entry.id, value = Unit)
            cache[key]?.put(key = entry.id, value = entry)

            launch { removeGarbage() }
        }.isSuccess
    }

    suspend fun select(
        cacheKey: CacheKey,
        entryKey: CacheEntryId
    ) = coroutineScope {

    }

    private suspend fun removeGarbage() = supervisorScope {
        try {
            garbageCache.forEach { (expiresAt, cacheInfo) ->
                removeByCacheKey(expiresAt, cacheInfo)
            }

        } catch (_: Throwable) {
            TODO("ADD LOG")
        }
    }

    private fun CoroutineScope.removeByCacheKey(
        expiresAt: Instant,
        cacheInfo: ConcurrentHashMap<CacheKey, ConcurrentHashMap<CacheEntryId, Unit>>
    ) = launch {
        if (expiresAt <= Clock.System.now()) {
            cacheInfo.forEach { (cacheKey, cacheEntryId) ->
                removeByCacheEntryId(cacheKey, cacheEntryId)
            }
        }
    }

    private fun CoroutineScope.removeByCacheEntryId(
        cacheKey: CacheKey,
        garbageEntryId: ConcurrentHashMap<CacheEntryId, Unit>
    ) = launch {
        garbageEntryId.forEach { (cacheEntryId, _) ->
            cache[cacheKey]?.remove(cacheEntryId)
        }
    }
}