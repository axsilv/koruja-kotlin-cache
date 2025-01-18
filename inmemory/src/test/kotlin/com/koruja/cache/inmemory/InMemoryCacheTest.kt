package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheProperties
import com.koruja.cache.core.Decorator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes

class InMemoryCacheTest :
    StringSpec({

        "insert should add entry to cache" {
            val mockExpirationDecider = mockk<InMemoryExpirationDecider>()
            val mockProperties = mockk<CacheProperties>()
            val insertDecorators = emptyList<Decorator>()
            val selectDecorators = emptyList<Decorator>()
            val scope = CoroutineScope(Dispatchers.Default)
            val cache = ConcurrentHashMap<CacheEntry.CacheEntryKey, CacheEntry>()
            val cacheExpirations = ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>>()
            val inMemoryCache =
                InMemoryCache(
                    properties = mockProperties,
                    expirationDecider = mockExpirationDecider,
                    insertDecorators = insertDecorators,
                    selectDecorators = selectDecorators,
                    scope = scope,
                    cache = cache,
                    cacheExpirations = cacheExpirations,
                )
            val key = CacheEntry.CacheEntryKey("test")
            val entry = CacheEntry(key, Clock.System.now().plus(2.minutes), payload = "payload")

            runTest {
                inMemoryCache.insert(entry).isSuccess shouldBe true
                cache[key] shouldBe entry
            }
        }

        "select should return cached entry" {
            val mockExpirationDecider = mockk<InMemoryExpirationDecider>()
            val mockProperties = mockk<CacheProperties>()
            val mockInsertDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val mockSelectDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val scope = CoroutineScope(Dispatchers.Default)
            val cache = ConcurrentHashMap<CacheEntry.CacheEntryKey, CacheEntry>()
            val cacheExpirations = ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>>()
            val inMemoryCache =
                InMemoryCache(
                    properties = mockProperties,
                    expirationDecider = mockExpirationDecider,
                    insertDecorators = mockInsertDecorators,
                    selectDecorators = mockSelectDecorators,
                    scope = scope,
                    cache = cache,
                    cacheExpirations = cacheExpirations,
                )

            val key = CacheEntry.CacheEntryKey("test")
            val entry = CacheEntry(key, Clock.System.now().plus(2.minutes), payload = "payload")
            cache[key] = entry

            runTest {
                val result = inMemoryCache.select(key)
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe entry
            }
        }

        "select should return null for expired entry" {
            val mockExpirationDecider = mockk<InMemoryExpirationDecider>()
            val mockProperties = mockk<CacheProperties>()
            val mockInsertDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val mockSelectDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val scope = CoroutineScope(Dispatchers.Default)
            val cache = ConcurrentHashMap<CacheEntry.CacheEntryKey, CacheEntry>()
            val cacheExpirations = ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>>()
            val inMemoryCache =
                InMemoryCache(
                    properties = mockProperties,
                    expirationDecider = mockExpirationDecider,
                    insertDecorators = mockInsertDecorators,
                    selectDecorators = mockSelectDecorators,
                    scope = scope,
                    cache = cache,
                    cacheExpirations = cacheExpirations,
                )
            val key = CacheEntry.CacheEntryKey("test")
            val entry = CacheEntry(key, Instant.parse("2024-01-01T00:00:00Z"), payload = "payload") // expired
            cache[key] = entry

            runTest {
                val result = inMemoryCache.select(key)
                result.isSuccess shouldBe true
                result.getOrNull().shouldBeNull()
            }
        }

        "cleanAll should remove all entries" {
            val mockExpirationDecider = mockk<InMemoryExpirationDecider>()
            val mockProperties = mockk<CacheProperties>()
            val mockInsertDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val mockSelectDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val scope = CoroutineScope(Dispatchers.Default)
            val cache = ConcurrentHashMap<CacheEntry.CacheEntryKey, CacheEntry>()
            val cacheExpirations = ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>>()
            val inMemoryCache =
                InMemoryCache(
                    properties = mockProperties,
                    expirationDecider = mockExpirationDecider,
                    insertDecorators = mockInsertDecorators,
                    selectDecorators = mockSelectDecorators,
                    scope = scope,
                    cache = cache,
                    cacheExpirations = cacheExpirations,
                )
            val key1 = CacheEntry.CacheEntryKey("key1")
            val key2 = CacheEntry.CacheEntryKey("key2")
            val entry1 = CacheEntry(key1, Clock.System.now().plus(2.minutes), payload = "payload")
            val entry2 = CacheEntry(key2, Clock.System.now().plus(2.minutes), payload = "payload")
            cache[key1] = entry1
            cache[key2] = entry2

            runTest {
                inMemoryCache.cleanAll().isSuccess shouldBe true
                cache.isEmpty() shouldBe true
            }
        }

        "insertAsync should handle multiple entries" {
            val mockExpirationDecider = mockk<InMemoryExpirationDecider>()
            val mockProperties = mockk<CacheProperties>()
            val insertDecorators = emptyList<Decorator>()
            val selectDecorators = emptyList<Decorator>()
            val scope = CoroutineScope(Dispatchers.Default)
            val cache = ConcurrentHashMap<CacheEntry.CacheEntryKey, CacheEntry>()
            val cacheExpirations = ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>>()
            val inMemoryCache =
                InMemoryCache(
                    properties = mockProperties,
                    expirationDecider = mockExpirationDecider,
                    insertDecorators = insertDecorators,
                    selectDecorators = selectDecorators,
                    scope = scope,
                    cache = cache,
                    cacheExpirations = cacheExpirations,
                )
            val key1 = CacheEntry.CacheEntryKey("key1")
            val key2 = CacheEntry.CacheEntryKey("key2")
            val entry1 = CacheEntry(key1, Clock.System.now().plus(2.minutes), payload = "payload")
            val entry2 = CacheEntry(key2, Clock.System.now().plus(2.minutes), payload = "payload")

            runTest {
                inMemoryCache.insertAsync(listOf(entry1, entry2)).isSuccess shouldBe true
                cache[key1] shouldBe entry1
                cache[key2] shouldBe entry2
            }
        }

        "selectAll should return all entries" {
            val mockExpirationDecider = mockk<InMemoryExpirationDecider>()
            val mockProperties = mockk<CacheProperties>()
            val mockInsertDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val mockSelectDecorators = listOf(mockk<Decorator>(), mockk<Decorator>())
            val scope = CoroutineScope(Dispatchers.Default)
            val cache = ConcurrentHashMap<CacheEntry.CacheEntryKey, CacheEntry>()
            val cacheExpirations = ConcurrentHashMap<Instant, ConcurrentLinkedQueue<CacheEntry.CacheEntryKey>>()
            val inMemoryCache =
                InMemoryCache(
                    properties = mockProperties,
                    expirationDecider = mockExpirationDecider,
                    insertDecorators = mockInsertDecorators,
                    selectDecorators = mockSelectDecorators,
                    scope = scope,
                    cache = cache,
                    cacheExpirations = cacheExpirations,
                )
            val key1 = CacheEntry.CacheEntryKey("key1")
            val key2 = CacheEntry.CacheEntryKey("key2")
            val entry1 = CacheEntry(key1, Clock.System.now().plus(2.minutes), payload = "payload")
            val entry2 = CacheEntry(key2, Clock.System.now().plus(2.minutes), payload = "payload")
            cache[key1] = entry1
            cache[key2] = entry2

            runTest {
                val result = inMemoryCache.selectAll()
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe listOf(entry1, entry2)
            }
        }
    })
