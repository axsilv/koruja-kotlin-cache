package com.koruja.cache

import com.koruja.cache.CacheEntry.CacheEntryKey
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

object CacheTestFixture {
    fun entries() =
        setOf(
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
            CacheEntry(
                id = CacheEntryKey(UUID.randomUUID().toString()),
                expiresAt = Clock.System.now().plus(5.minutes),
                payload = "test",
            ),
        )
}
