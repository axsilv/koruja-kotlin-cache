package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry
import com.koruja.cache.core.CacheEntry.CacheEntryKey
import com.koruja.cache.inmemory.CacheTestFixture.entries
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class InMemoryCacheTest :
    BehaviorSpec({

        context("Concurrent singleton cache") {
            given("N concurrent insert operations") {
                `when`("Insert") {
                    then("Should contain all elements") {
                        val inMemoryCache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                        entries().map { entry ->
                            inMemoryCache.insert(entry = entry, expiresAt = entry.expiresAt)
                        }

                        inMemoryCache.selectAll().getOrNull()?.size shouldBe 10
                    }
                }

                `when`("Insert Async") {
                    then("Should contain all elements") {
                        val inMemoryCache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                        entries().map { entry ->
                            inMemoryCache.insertAsync(entry = entry, expiresAt = entry.expiresAt)
                        }

                        inMemoryCache.selectAll().getOrNull()?.size shouldBe 10
                    }
                }

                `when`("Launch Insert") {
                    then("Should contain all elements") {
                        val inMemoryCache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                        entries().map { entry ->
                            inMemoryCache.launchInsert(entry = entry, expiresAt = entry.expiresAt)
                        }

                        inMemoryCache.selectAll().getOrNull()?.size shouldBe 10
                    }
                }

                `when`("Delete") {
                    then("Should remove by expiration") {
                        val inMemoryCache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                        inMemoryCache.insert(
                            entry =
                                CacheEntry(
                                    key = CacheEntryKey("key-test"),
                                    payload = "payload test",
                                    expiresAt = Clock.System.now().plus(2.seconds),
                                ),
                            expiresAt = Clock.System.now().plus(2.seconds),
                        )

                        inMemoryCache.insert(
                            entry =
                                CacheEntry(
                                    key = CacheEntryKey("key-test2"),
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

                    then("Should remove by clean all") {
                        val inMemoryCache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                        inMemoryCache.insert(
                            entry =
                                CacheEntry(
                                    key = CacheEntryKey("key-test"),
                                    payload = "payload test",
                                    expiresAt = Clock.System.now().plus(2.days),
                                ),
                            expiresAt = Clock.System.now().plus(2.days),
                        )

                        inMemoryCache.insert(
                            entry =
                                CacheEntry(
                                    key = CacheEntryKey("key-test2"),
                                    payload = "payload test 2",
                                    expiresAt = Clock.System.now().plus(5.days),
                                ),
                            expiresAt = Clock.System.now().plus(5.days),
                        )

                        inMemoryCache.select(CacheEntryKey("key-test")).getOrNull().shouldNotBeNull()
                        inMemoryCache.select(CacheEntryKey("key-test2")).getOrNull().shouldNotBeNull()

                        inMemoryCache.cleanAll()

                        inMemoryCache.select(CacheEntryKey("key-test")).getOrNull().shouldBeNull()
                        inMemoryCache.select(CacheEntryKey("key-test2")).getOrNull().shouldBeNull()
                    }
                }
            }
        }
    })
