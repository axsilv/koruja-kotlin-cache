package com.koruja.cache

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

class CacheTest : BehaviorSpec({

	context("Concurrent singleton cache") {
		given("N concurrent insert operations") {
			`when`("Insert") {
				then("Should contain all elements") {
					val cache = Cache()

					entries().map { entry ->
						launch {
							cache.insert(entry = entry, expiresAt = entry.expiresAt)
						}
					}.joinAll()

					cache.selectAll().getOrThrow().let {
						it.size shouldBe 10
					}
				}
			}

			`when`("Insert Async") {
				then("Should contain all elements") {
					val cache = Cache()

					entries().map { entry ->
						cache.insertAsync(entry = entry, expiresAt = entry.expiresAt)
					}.awaitAll()

					cache.selectAll().getOrThrow().let {
						it.size shouldBe 10
					}
				}
			}

			`when`("Launch Insert") {
				then("Should contain all elements") {
					val cache = Cache()

					entries().map { entry ->
						cache.launchInsert(entry = entry, expiresAt = entry.expiresAt)
					}.joinAll()

					cache.selectAll().getOrThrow().let {
						it.size shouldBe 10
					}
				}
			}

			`when`("Delete") {
				then("Should remove by expiration") {
					val cache = Cache()

					cache.insert(
						entry =
							CacheEntry(
								id = CacheEntryKey("key-test"),
								payload = "payload test",
								expiresAt = Clock.System.now().plus(2.seconds),
							),
						expiresAt = Clock.System.now().plus(2.seconds),
					)

					cache.insert(
						entry =
							CacheEntry(
								id = CacheEntryKey("key-test2"),
								payload = "payload test 2",
								expiresAt = Clock.System.now().plus(5.seconds),
							),
						expiresAt = Clock.System.now().plus(5.seconds),
					)

					cache.select(CacheEntryKey("key-test")).getOrNull().shouldNotBeNull()
					cache.select(CacheEntryKey("key-test2")).getOrNull().shouldNotBeNull()

					delay(3.seconds)

					cache.select(CacheEntryKey("key-test")).getOrNull().shouldBeNull()
					cache.select(CacheEntryKey("key-test2")).getOrNull().shouldNotBeNull()

					delay(3.seconds)

					cache.select(CacheEntryKey("key-test2")).getOrNull().shouldBeNull()
				}
			}
		}
	}
})
