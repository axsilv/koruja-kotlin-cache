package com.koruja.cache.inmemory

import com.koruja.cache.core.CacheEntry.CacheEntryKey
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.minutes

class InMemoryExpirationDeciderGenericTest :
    StringSpec({

        val decider = InMemoryExpirationDeciderGeneric()

        fun mockConcurrentLinkedQueue(): ConcurrentLinkedQueue<CacheEntryKey> = ConcurrentLinkedQueue()

        "should return true when expiresAt is in the past" {
            val expiredInstant = Clock.System.now().minus(2.minutes)

            val result =
                decider.shouldRemove(
                    keys = mockConcurrentLinkedQueue(),
                    expiresAt = expiredInstant,
                )

            result shouldBe true
        }

        "should return false when expiresAt is in the future" {
            val futureInstant = Clock.System.now().plus(2.minutes)

            val result =
                decider.shouldRemove(
                    keys = mockConcurrentLinkedQueue(),
                    expiresAt = futureInstant,
                )

            result shouldBe false
        }

        "should return true when expiresAt is now" {
            val currentInstant = Clock.System.now()

            val result =
                decider.shouldRemove(
                    keys = mockConcurrentLinkedQueue(),
                    expiresAt = currentInstant,
                )

            result shouldBe true
        }
    })
