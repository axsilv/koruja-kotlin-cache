package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.Decider
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.datetime.Instant

interface InMemoryExpirationDecider : Decider {
    fun shouldRemove(keys: ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>, expiresAt: Instant): Boolean
}