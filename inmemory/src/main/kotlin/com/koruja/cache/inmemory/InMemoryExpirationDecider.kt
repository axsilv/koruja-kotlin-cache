package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.Decider
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentLinkedQueue

interface InMemoryExpirationDecider : Decider {
    fun shouldRemove(
        keys: ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>,
        expiresAt: Instant,
    ): Boolean
}
