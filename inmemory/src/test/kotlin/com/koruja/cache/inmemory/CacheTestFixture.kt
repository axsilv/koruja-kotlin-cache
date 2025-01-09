package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

object CacheTestFixture {
    fun entries() =
        setOf(
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                key = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
        )
}
