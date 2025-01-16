package com.koruja.cache.core

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class CacheEntryTest :
    BehaviorSpec({

        val sampleKey = CacheEntry.CacheEntryKey("test-key")
        val samplePayload = "Sample Payload"
        val sampleInstant = Instant.parse("2025-01-01T00:00:00Z")

        given("a CacheEntry instance") {
            val cacheEntry = CacheEntry(sampleKey, sampleInstant, samplePayload)

            `when`("accessing its properties") {
                then("it should correctly return the key") {
                    cacheEntry.key shouldBe sampleKey
                }
                then("it should correctly return the expiration time") {
                    cacheEntry.expiresAt shouldBe sampleInstant
                }
                then("it should correctly return the payload") {
                    cacheEntry.payload shouldBe samplePayload
                }
            }

            `when`("serializing and deserializing the instance") {
                val json = Json.encodeToString(cacheEntry)
                val deserializedCacheEntry = Json.decodeFromString<CacheEntry>(json)

                then("it should maintain equality") {
                    deserializedCacheEntry shouldBe cacheEntry
                }
                then("the deserialized object should be an instance of CacheEntry") {
                    deserializedCacheEntry.shouldBeInstanceOf<CacheEntry>()
                }
            }
        }

        given("a CacheEntryKey instance") {
            val keyInstance = CacheEntry.CacheEntryKey("my-id")

            `when`("converting to string") {
                val result = keyInstance.toString()

                then("it should return the correct id string") {
                    result shouldBe "my-id"
                }
            }
        }

        given("mocked CacheEntry objects") {
            val mockKey = mockk<CacheEntry.CacheEntryKey>(relaxed = true)
            val mockExpiresAt = Clock.System.now()
            val mockPayload = "Mocked Payload"
            val mockedEntry = CacheEntry(mockKey, mockExpiresAt, mockPayload)

            then("it should allow property verification") {
                mockedEntry.key shouldBe mockKey
                mockedEntry.expiresAt shouldBe mockExpiresAt
                mockedEntry.payload shouldBe mockPayload
            }
        }
    })
