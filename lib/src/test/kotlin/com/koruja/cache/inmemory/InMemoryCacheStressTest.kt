package com.koruja.cache.inmemory

import com.koruja.cache.CacheEntry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.util.UUID
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

class InMemoryCacheStressTest : BehaviorSpec({
    xcontext("Writing and reading stress tests") {
        given("Multiple inserts syn, async and launch") {
            `when`("Expiration worker runs a lot of data") {
                then("Should not crash or leak") {
                    val cases = (1..10000000).toList()
                    val cache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                    cases.forEach { _ ->
                        val expiresAt = 2
                        cache.launchInsert(
                            CacheEntry(
                                key = CacheEntry.CacheEntryKey(UUID.randomUUID().toString()),
                                payload = "Stress test",
                                expiresAt = instant(expiresAt)
                            ),
                            expiresAt = instant(expiresAt)
                        )
                    }

                    cases.forEach { _ ->
                        val expiresAt = 3
                        cache.insertAsync(
                            CacheEntry(
                                key = CacheEntry.CacheEntryKey(UUID.randomUUID().toString()),
                                payload = "Stress test",
                                expiresAt = instant(expiresAt)
                            ),
                            expiresAt = instant(expiresAt)
                        )
                    }

                    cases.forEach { _ ->
                        val expiresAt = 4
                        cache.insert(
                            CacheEntry(
                                key = CacheEntry.CacheEntryKey(UUID.randomUUID().toString()),
                                payload = "Stress test",
                                expiresAt = instant(expiresAt)
                            ),
                            expiresAt = instant(expiresAt)
                        )
                    }

                    (0..5).toList().forEach { time ->
                        val selectAllSize = cache.selectAll().size

                        when (time) {
                            1 -> selectAllSize shouldBe 30000000
                            2 -> selectAllSize shouldBe 20000000
                            3 -> selectAllSize shouldBe 10000000
                            in 4..5 -> selectAllSize shouldBe 0
                        }

                        delay(1.minutes)
                    }
                }
            }
        }

        given("Sync inserts") {
            `when`("Multiple inserts") {
                then("Measure times") {
                    val cases = (1..10000000).toList()
                    val cache = InMemoryCache(expirationDecider = InMemoryExpirationDeciderGeneric())

                    val duration = measureTimeMillis {
                        cases.forEach { _ ->
                            val expiresAt = 2
                            cache.insert(
                                CacheEntry(
                                    key = CacheEntry.CacheEntryKey(UUID.randomUUID().toString()),
                                    payload = "Stress test",
                                    expiresAt = instant(expiresAt)
                                ),
                                expiresAt = instant(expiresAt)
                            )
                        }
                    }

                    duration shouldBeLessThanOrEqual 10000
                }
            }
        }
    }
})

private fun instant(expiresAt: Int) = Clock.System.now().plus(expiresAt.minutes)