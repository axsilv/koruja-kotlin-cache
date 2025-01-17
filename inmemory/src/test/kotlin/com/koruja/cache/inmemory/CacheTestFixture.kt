package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

object CacheTestFixture {
    fun entries() =
        (1..50000).map {
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(1.minutes),
                payload = UUID.randomUUID().toString() + UUID.randomUUID().toString(),
            )
        }
}
