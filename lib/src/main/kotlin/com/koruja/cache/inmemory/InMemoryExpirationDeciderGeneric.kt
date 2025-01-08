package com.koruja.cache.inmemory

import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.InMemoryExpirationDecider
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class InMemoryExpirationDeciderGeneric : InMemoryExpirationDecider {

    override fun shouldRemove(
        keys: ConcurrentLinkedQueue<CacheEntryKey>,
        expiresAt: Instant
    ): Boolean = expiresAt <= Clock.System.now()
}
