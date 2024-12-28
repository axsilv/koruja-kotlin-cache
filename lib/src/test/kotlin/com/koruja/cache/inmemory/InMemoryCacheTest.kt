package com.koruja.cache.inmemory

import com.koruja.cache.CacheEntry
import com.koruja.cache.CacheEntry.CacheEntryKey
import com.koruja.cache.CacheTestFixture.entries
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class InMemoryCacheTest : BehaviorSpec({

    context("Concurrent singleton cache") {
        given("N concurrent insert operations") {
            `when`("Insert") {
                then("Should contain all elements") {
                    val inMemoryCache = InMemoryCache()

                    entries().map { entry ->
                        launch {
                            inMemoryCache.insert(entry = entry, expiresAt = entry.expiresAt)
                        }
                    }.joinAll()

                    inMemoryCache.selectAll().getOrThrow().let {
                        it.size shouldBe 10
                    }
                }
            }

            `when`("Insert Async") {
                then("Should contain all elements") {
                    val inMemoryCache = InMemoryCache()

                    entries().map { entry ->
                        inMemoryCache.insertAsync(entry = entry, expiresAt = entry.expiresAt)
                    }.forEach { it.getOrNull()?.await() }

                    inMemoryCache.selectAll().getOrThrow().let {
                        it.size shouldBe 10
                    }
                }
            }

            `when`("Launch Insert") {
                then("Should contain all elements") {
                    val inMemoryCache = InMemoryCache()

                    entries().map { entry ->
                        inMemoryCache.launchInsert(entry = entry, expiresAt = entry.expiresAt)
                    }.joinAll()

                    inMemoryCache.selectAll().getOrThrow().let {
                        it.size shouldBe 10
                    }
                }
            }

            `when`("Delete") {
                then("Should remove by expiration") {
                    val inMemoryCache = InMemoryCache()

                    inMemoryCache.insert(
                        entry =
                            CacheEntry(
                                id = CacheEntryKey("key-test"),
                                payload = "payload test",
                                expiresAt = Clock.System.now().plus(2.seconds),
                            ),
                        expiresAt = Clock.System.now().plus(2.seconds),
                    )

                    inMemoryCache.insert(
                        entry =
                            CacheEntry(
                                id = CacheEntryKey("key-test2"),
                                payload = "payload test 2",
                                expiresAt = Clock.System.now().plus(5.seconds),
                            ),
                        expiresAt = Clock.System.now().plus(5.seconds),
                    )

                    inMemoryCache.select(CacheEntryKey("key-test")).getOrNull().shouldNotBeNull()
                    inMemoryCache.select(CacheEntryKey("key-test2")).getOrNull().shouldNotBeNull()

                    delay(3.seconds)

                    inMemoryCache.select(CacheEntryKey("key-test")).getOrNull().shouldBeNull()
                    inMemoryCache.select(CacheEntryKey("key-test2")).getOrNull().shouldNotBeNull()

                    delay(3.seconds)

                    inMemoryCache.select(CacheEntryKey("key-test2")).getOrNull().shouldBeNull()
                }
            }
        }
    }
})
