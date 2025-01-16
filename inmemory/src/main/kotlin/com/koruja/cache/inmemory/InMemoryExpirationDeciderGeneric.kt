package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry.CacheEntryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class InMemoryExpirationDeciderGeneric : InMemoryExpirationDecider {
    override fun shouldRemove(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant,
    ): Boolean = expiresAt <= Clock.System.now()
}
