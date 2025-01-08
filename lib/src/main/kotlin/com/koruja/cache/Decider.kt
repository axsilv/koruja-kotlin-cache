package com.koruja.cache

import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.datetime.Instant

interface Decider

interface InMemoryExpirationDecider : Decider {
    fun shouldRemove(keys: ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>, expiresAt: Instant): Boolean
}
