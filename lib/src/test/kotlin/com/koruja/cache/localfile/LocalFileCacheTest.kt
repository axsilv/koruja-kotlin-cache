package com.koruja.cache.localfile

import com.koruja.cache.CacheEntry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.datetime.Clock

class LocalFileCacheTest : BehaviorSpec({

    context("Writes local cache in files at the root") {
        given("A single cache entry") {
            `when`("Insert sync the cache") {
                then("Should be able to insert, select, select all and then expires the cache") {
                    val properties = LocalFileCacheProperties(
                        baseDir = listOf("src", "test", "resources"),
                    )
                    val cache = LocalFileCache(properties = properties)
                    val key = UUID.randomUUID().toString()
                    val expiresAt = Clock.System.now().plus(30.seconds)

                    cache.insert(
                        entry = CacheEntry(
                            id = CacheEntry.CacheEntryKey(key),
                            expiresAt = expiresAt,
                            payload = "test=payload"
                        ),
                        expiresAt = expiresAt
                    )

                    Files.exists(
                        Paths.get("src", "test", "resources", "cache", "$key.txt")
                    ) shouldBe true

                    Files.exists(
                        Paths.get(
                            "src", "test", "resources", "expirations",
                            expiresAt.toString().replace(":", "_"), "$key.txt"
                        )
                    ) shouldBe true

                    val (keys, duration) = measureTimedValue {
                        (1..2000).map { _ ->
                            val randomKey = UUID.randomUUID().toString()
                            cache.insert(
                                entry = CacheEntry(
                                    id = CacheEntry.CacheEntryKey(randomKey),
                                    expiresAt = expiresAt,
                                    payload = "test=payload"
                                ),
                                expiresAt = expiresAt
                            )

                            randomKey
                        }
                    }

                    duration shouldBeLessThan 10.seconds

                    keys.forEach {
                        cache.select(CacheEntry.CacheEntryKey(it)).shouldNotBeNull()
                    }

                    cache.cleanAll().await()
                }
            }
        }
    }
})
